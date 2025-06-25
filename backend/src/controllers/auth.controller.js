import { upsertStreamUser } from '../lib/stream.js';
import User from '../models/User.js'; // Adjust the import path as necessary
import jwt from "jsonwebtoken"; // Import jsonwebtoken for token generation

export async function signup(req, res) {
  // Handle user signup logic here
  const {email,password,fullName } = req.body;
  
  try {
    
    if(!email || !password || !fullName) {
      return res.status(400).json({ message: "All fields are required" });
    }

    if(password.length < 6) {
      return res.status(400).json({ message: "Password must be at least 6 characters long" });
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailRegex.test(email)) {
      return res.status(400).json({ message: "Invalid email format" });
    }
    
    const existingUser = await User.findOne({ email });
    if (existingUser) {
      return res.status(400).json({ message:"User already exists with this email, please use a different email" });
    } 

    const idx = Math.floor(Math.random() * 100) + 1; // Generate number between 1 and 100
    const randomAvatar = `https://avatar.iran.liara.run/public/${idx}.png`; // Use the generated number in the URL

    const newUser = await User.create({
      email,
      fullName,
      password,
      profilePic: randomAvatar,
    })

    try {
      await upsertStreamUser({
      id: newUser._id.toString(), // Use the user's ID as the Stream user ID
      name: newUser.fullName, // Use the user's full name as the Stream user name
      image: newUser.profilePic || "", // Use the user's profile picture as the Stream user image
    })
    console.log(`Stream user upserted successfully for ${newUser.fullName}`);
    } catch (error) {
      console.error("Error upserting Stream user:", error);
    }

    const token = jwt.sign({userId: newUser._id}, process.env.JWT_SECRET_KEY, {
      expiresIn: '7d' // Token will expire in 7 day)
    })

    res.cookie("jwt", token, {
      maxAge: 7 * 24 * 60 * 60 * 1000, // 7 days in milliseconds
      httpOnly: true, // Prevents client-side JavaScript from accessing the cookie (Prevent XSS attacks)
      sameSite: 'strict', // Helps prevent CSRF attacks
      secure: process.env.NODE_ENV === 'production', // Use secure cookies in production
    }) 

    res.status(201).json({sucess: true, user: newUser}); // Send a response to the client with the new user data

  } catch (error) {
    console.log("Error in signup controller:", error);
    res.status(500).json({ message: "Internal server error" }); // Send a 500 error response if something goes wrong
  }
}

export async function login(req, res) {
  // Handle user login logic here
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ message: "Email and password are required" });
    }

    const user = await User.findOne({ email });
    if (!user) return res.status(401).json({ message: "Invalid email or password" });

    const isPasswordCorrect = await user.matchPassword(password); // Assuming you have a method to compare passwords
    if(!isPasswordCorrect) return res.status(401).json({ message: "Invalid email or password" });

    const token = jwt.sign({userId: user._id}, process.env.JWT_SECRET_KEY, {
      expiresIn: '7d' // Token will expire in 7 day)
    })

    res.cookie("jwt", token, {
      maxAge: 7 * 24 * 60 * 60 * 1000, // 7 days in milliseconds
      httpOnly: true, // Prevents client-side JavaScript from accessing the cookie (Prevent XSS attacks)
      sameSite: 'strict', // Helps prevent CSRF attacks
      secure: process.env.NODE_ENV === 'production', // Use secure cookies in production
    }) 

    res.status(200).json({ success: true, user }); // Send a response to the client with the user data

  } catch (error) {
    console.log("Error in login controller:", error.message);
    res.status(500).json({ message: "Internal server error" }); // Send a 500 error response if something goes wrong
  }
}

export function logout(req, res) {
  // Handle user logout logic here
  res.clearCookie("jwt")
  res.status(200).json({ success: true, message: "Logged out successfully" }); // Clear the cookie and send a response
  // You can also invalidate the token on the server side if needed
}

export async function onboard(req, res) {
  try {
    const userId = req.user._id; // Get the user ID from the request object
    const { fullName, bio, nativeLanguage, learningLanguage ,location} = req.body; // Get the full name and profile picture from the request body
    if(!fullName || !bio || !nativeLanguage || !learningLanguage || !location) {
      return res.status(400).json({ message: "All fields are required",
        missingFields: [
          !fullName && "fullName",
          !bio && "bio",
          !nativeLanguage && "nativeLanguage",
          !learningLanguage && "learningLanguage",
          !location && "location"
        ].filter(Boolean), // Filter out any undefined values,
       });
    }

    const updatedUser = await User.findByIdAndUpdate(userId, {
      ...req.body, // Update the user with the data from the request body
      isOnboarded: true, // Set the isOnboarded field to true
    }, { new: true }); // Return the updated user object

    if (!updatedUser) {
      return res.status(404).json({ message: "User not found" });
    }
    
    try {
      await upsertStreamUser({
      id: updatedUser._id.toString(), // Use the user's ID as the Stream user ID
      name: updatedUser.fullName, // Use the user's full name as the Stream user name
      image: updatedUser.profilePic || "", // Use the user's profile picture as the Stream user image
    });
    console.log(`Stream user upserted successfully for ${updatedUser.fullName}`);
    } catch (streamError) {
      console.error("Error upserting Stream user during onboarding:", streamError.message);
      // You might want to handle this error differently, e.g., log it or notify the user
    }

    res.status(200).json({ success: true, user: updatedUser }); // Send a response to the client with the updated user data
  } catch (error) {
    console.error("Error in onboard controller:", error);
    res.status(500).json({ message: "Internal server error" }); // Send a 500 error response if something goes wrong
  }
}