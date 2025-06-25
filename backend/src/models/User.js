import mongoose from "mongoose";
import bcrypt from "bcryptjs";

const userSchema = new mongoose.Schema({
    fullName:{
        type: String,
        required: true,
    },
    email: {
        type: String,
        required: true,
        unique: true,
    },
    password:{
        type: String,
        required: true,
        minLength: 6,
    },
    bio: {
        type: String,
        default: "",
    },
    profilePic: {
        type: String,
        default: "",
    },
    nativeLanguage: {
        type: String,
        default: "",
    },
    learningLanguage: {
        type: String,
        default: "",
    },
    location: {
        type: String,
        default: "",
    },
    isOnboarded: {
        type: Boolean,
        default: false,
    },
    friends: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: "User",
    }],
}, {timestamps: true});
// createdAt, updatedAt

// Pre hook
userSchema.pre("save", async function(next) {// Before saving the user has the password field
    // If password is not modified, skip hashing 12345 => 12345hash625
    if(!this.isModified("password")) return next();// If user is trying to modify something other than password, skip hashing
    // If password is not modified, skip hashing 12345 => 12345hash625
    try{
        const salt = await bcrypt.genSalt(10);
        this.password = await bcrypt.hash(this.password, salt);
        next();
    }catch (error) {
        next(error);
    }
});

userSchema.methods.matchPassword = async function(candidatePassword) {
    // Compare the candidate password with the hashed password
    const isPasswordCorrect = await bcrypt.compare(candidatePassword, this.password);
    return isPasswordCorrect; // Return true if passwords match, false otherwise
};

const User = mongoose.model("User", userSchema);


export default User;