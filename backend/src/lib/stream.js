import {StreamChat} from 'stream-chat';
import "dotenv/config";

const apikey = process.env.STREAM_API_KEY;
const apiSecret = process.env.STREAM_API_SECRET;

if(!apikey || !apiSecret) {
  console.error("Stream API key or Secret must be set in the environment variables.");
}

const streamClient = StreamChat.getInstance(apikey, apiSecret);

export const upsertStreamUser = async (userData) => {
    try {
        await streamClient.upsertUsers([userData]);// If it doesn't exist, it will create a new user, if it exists, it will update the user.
        return userData;
    } catch (error) {
        console.error("Error upserting Stream user:", error);
    }
}

export const generateStreamToken = (userId) => {
    try {
        // Ensure the userId is a string
        const userIdstr = String(userId);
        // Generate a token for the user
        return streamClient.createToken(userIdstr);
    } catch (error) {
        console.error("Error generating Stream token:", error);
    }
};