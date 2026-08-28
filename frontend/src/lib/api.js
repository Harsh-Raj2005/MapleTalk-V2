import { axiosInstance } from "./axios";

export const signup = async (signupData) => {
  const response = await axiosInstance.post("/auth/signup", signupData);
  return response.data;
};

export const login = async (loginData) => {
  const response = await axiosInstance.post("/auth/login", loginData);
  return response.data;
};

export const getAuthUser = async () => {
  try {
    const res = await axiosInstance.get("/users/me");
    return res.data;
  } catch (error) {
    console.log("Error in getAuthUser:", error);
    return null;
  }
};

export const completeOnboarding = async (userData) => {
  // V2's OnboardingRequest only accepts these five fields.
  const { fullName, bio, nativeLanguage, learningLanguage, location } = userData;
  const response = await axiosInstance.put("/users/onboarding", {
    fullName,
    bio,
    nativeLanguage,
    learningLanguage,
    location,
  });
  return response.data;
};

export async function getUserFriends() {
  const response = await axiosInstance.get("/friends");
  return response.data;
}

export async function getRecommendedUsers() {
  const response = await axiosInstance.get("/users/recommended");
  return response.data;
}

export async function getFriendRequests() {
  const response = await axiosInstance.get("/friends/requests");
  return response.data;
}

export async function sendFriendRequest(userId) {
  const response = await axiosInstance.post(`/friends/requests/${userId}`);
  return response.data;
}

export async function acceptFriendRequest(requestId) {
  const response = await axiosInstance.put(`/friends/requests/${requestId}/accept`);
  return response.data;
}

export async function rejectFriendRequest(requestId) {
  const response = await axiosInstance.put(`/friends/requests/${requestId}/reject`);
  return response.data;
}

export async function getStreamToken() {
  const response = await axiosInstance.get("/chat/token");
  return response.data;
}
