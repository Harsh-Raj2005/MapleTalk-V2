package com.mapletalk.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String fullName;

	@Column(nullable = false, unique = true)
	private String email;

	// Stores a BCrypt hash once Spring Security is introduced in a later phase.
	// Never populated with plaintext.
	@Column(name = "password_hash", nullable = false, length = 255)
	private String password;

	private String profilePic;

	@Column(columnDefinition = "TEXT")
	private String bio;

	private String nativeLanguage;

	private String learningLanguage;

	private String location;

	@Column(nullable = false)
	private boolean isOnboarded = false;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected User() {
		// required by JPA
	}

	public User(String fullName, String email, String password) {
		this.fullName = fullName;
		this.email = email;
		this.password = password;
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getProfilePic() {
		return profilePic;
	}

	public void setProfilePic(String profilePic) {
		this.profilePic = profilePic;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getNativeLanguage() {
		return nativeLanguage;
	}

	public void setNativeLanguage(String nativeLanguage) {
		this.nativeLanguage = nativeLanguage;
	}

	public String getLearningLanguage() {
		return learningLanguage;
	}

	public void setLearningLanguage(String learningLanguage) {
		this.learningLanguage = learningLanguage;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public boolean isOnboarded() {
		return isOnboarded;
	}

	public void setOnboarded(boolean onboarded) {
		isOnboarded = onboarded;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
