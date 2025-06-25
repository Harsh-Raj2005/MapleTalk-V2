import express from 'express';
import { protectRoute } from '../middleware/auth.middleware.js'; // Import the protectRoute middleware to protect routes
import { acceptFriendRequest, getFriendRequests, getMyFriends, getOutgoingFriendReqs, getRecommendedUsers, sendFriendRequest } from '../controllers/user.controller.js';

const router = express.Router();

router.use(protectRoute); // Apply the protectRoute middleware to all routes in this router

router.get("/" ,getRecommendedUsers);
router.get("/friends" ,getMyFriends);

router.post("/friend-request/:id", sendFriendRequest); // Send a friend request to a user by their ID
router.put("/friend-request/:id/accept", acceptFriendRequest); // Send a friend request to a user by their ID

router.get("/friend-requests", getFriendRequests); // Get all friend requests for the authenticated user 
router.get("/outgoing-friend-requests", getOutgoingFriendReqs); // Get all outgoing friend requests for the authenticated user


export default router;

// user.route.js
// This file defines the routes for user-related operations, such as sending and accepting friend requests, and getting friend requests.