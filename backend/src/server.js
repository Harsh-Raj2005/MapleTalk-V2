import express from 'express';// Express is our Web framework, this will allow us to build an API super easily quickly and it has every feature we expect from it.
import "dotenv/config"; // Import dotenv to manage environment variables
import cookieParser from 'cookie-parser'; // Import cookie-parser to handle cookies
import cors from 'cors'; // Import CORS to handle cross-origin requests
import path from 'path'; // Import path to handle file paths

import authRoutes from './routes/auth.route.js'; // Import the authentication routes
import userRoutes from './routes/user.route.js'; // Import the user routes
import chatRoutes from './routes/chat.route.js'; // Import the chat routes

import { connectDB } from './lib/db.js';
// Import the authentication routes from the auth.route.js file

const app = express(); // Create an instance of express
const PORT = process.env.PORT; // Get the port from environment variables

const __dirname = path.resolve(); // Get the current directory name

app.use(cors({
  origin: "http://localhost:5173", // Allow requests from this origin
  credentials: true, // Allow credentials (cookies, authorization headers, etc.) to be sent 
}));
app.use(express.json()); // Middleware to parse JSON bodies
app.use(cookieParser()); // Middleware to parse cookies

app.use("/api/auth", authRoutes); // Use the authentication routes under the /api/auth path
app.use("/api/users", userRoutes); // Use the authentication routes under the /api/auth path
app.use("/api/chat", chatRoutes); // Use the user routes under the /api/users path

if (process.env.NODE_ENV === "production") {
  app.use(express.static(path.join(__dirname, "../frontend/dist")));

  app.get("*", (req, res) => {
    res.sendFile(path.join(__dirname, "../frontend", "dist", "index.html"));
  });
}

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`); // Log to the console that the server is running
  connectDB(); // Connect to the database
});