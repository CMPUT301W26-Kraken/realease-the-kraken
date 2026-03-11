package com.example.releasethekraken.model;

/**
 * Profile model class representing a user's personal information in the app.
 *
 * This class acts as a simple data container (POJO) for profile information
 * entered by an entrant when creating their account. It currently stores
 * the user's name, email, and optional phone number.
 *
 * The fields are immutable (final) because once a Profile object is created,
 * its values should not change. If an update is needed, a new Profile object
 * can be created with the updated information.
 */
public class Profile {

    // User's full name (required field)
    private final String name;

    // User's email address (required field)
    private final String email;

    // User's phone number (optional field)
    private final String phone;


     //Constructs a new Profile object.

    public Profile(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone; // (can be empty if not provided)
    }


    //Returns the user's name.
    public String getName() {
        return name;
    }


    //Returns the user's email address.
    public String getEmail() {
        return email;
    }


    //Returns the user's phone number.(may be empty)
    public String getPhone() {
        return phone;
    }
}