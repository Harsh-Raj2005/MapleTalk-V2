import express from 'express'; // Import express to create a router
import { protectRoute } from '../middleware/auth.middleware.js';
import { getStreamToken } from '../controllers/chat.controller.js';

const router = express.Router(); // Create a new router instance

router.get("/token", protectRoute, getStreamToken); // Define a route to get the Stream token, protected by the auth middleware

export default router; // Export the router for use in other files