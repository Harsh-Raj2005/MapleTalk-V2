import express from 'express';
import { signup, login, logout, onboard } from '../controllers/auth.controller.js';    // Import the signup function from the auth.controller.js file
import { protectRoute } from '../middleware/auth.middleware.js';

const router = express.Router();

router.post('/signup', signup);
router.post('/login', login);
router.post('/logout', logout); //Post method is for operations that change the state of the server, like logging out a user
// That is why we use POST method for logout, even though it doesn't change the state of the server in a traditional sense.
// The logout route is used to clear the user's session or token, effectively logging them out of the application.

router.post("/onboarding", protectRoute ,onboard);

// Check if the user is authenticated before allowing access to the /me route
router.get("/me", protectRoute, (req, res) => {
  // This route is protected by the protectRoute middleware
  res.status(200).json({ success: true, user: req.user }); // Send the user data back to the client
});

export default router;

// auth.route.js
// This file defines the routes for user authentication and onboarding.