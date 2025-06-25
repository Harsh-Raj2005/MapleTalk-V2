import { generateStreamToken } from "../lib/stream.js";

export async function getStreamToken(req, res) { // This controller handles the request to get a Stream token
  try {
    const token = generateStreamToken(req.user.id);// Assuming req.user.id contains the authenticated user's ID

    res.status(200).json({ token });// Send the token back to the client
  } catch (error) {// Handle any errors that occur during token generation
    // Log the error for debugging purposes
    console.log("Error in getStreamToken controller:", error.message);
    res.status(500).json({ message: "Internal Server Error" });
  }
}