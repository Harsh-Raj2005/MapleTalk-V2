import jwt from 'jsonwebtoken';
import User from '../models/User.js'; // Adjust the import path as necessary


export const protectRoute = async (req, res, next) => {

    try {
        const token = req.cookies.jwt; // Get the JWT from cookies

        if(!token) {
            return res.status(401).json({ message: "Unauthorized - No token provided, please login" });
        }

        const decode = jwt.verify(token, process.env.JWT_SECRET_KEY); // Verify the token

        if(!decode) {
            return res.status(401).json({ message: "Unauthorized, invalid token, please login again" });
        }

        const user = await User.findById(decode.userId).select("-password"); // Find the user by ID from the token

        if(!user) {
            return res.status(401).json({ message: "User not found, please signup" });
        }

        req.user = user; // Attach the user to the request object

        next(); // Call the next middleware or route handler
    } catch (error) {
        console.error("Error in protectRoute middleware:", error);
        return res.status(500).json({ message: "Internal server error" });
    }
}