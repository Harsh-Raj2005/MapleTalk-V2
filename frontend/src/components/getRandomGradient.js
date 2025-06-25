// src/lib/getRandomGradient.js

const gradients = [
  "linear-gradient(to right, #F27C38, #FFB347)",                            // Sunset Flow
  "linear-gradient(to bottom right, #FDE4A3, #FFB347, #F27C38)",            // Warm Dawn
  "linear-gradient(to right, #8C4B2D, #F27C38, #FDE4A3)",                   // Earth & Sun
  "linear-gradient(to right, #FDE4A3, #F27C38, #8C4B2D)",                   // Earth & Sun flipped
  "linear-gradient(to top left, #FFB347, #FDE4A3)",                         // Soft Golden Ray
  "linear-gradient(to right, #FFF8F1, #FDE4A3, #FFB347)",                   // Sun-kissed Cream
  "linear-gradient(135deg, #FFF8F1, #FFB347, #F27C38)",                     // Soft diagonal
];

export function getRandomGradient() {
  const index = Math.floor(Math.random() * gradients.length);
  return gradients[index];
}