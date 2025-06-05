package com.project.tecniswim.ui.evaluate;

/**
 * Model sencillo para representar un usuario almacenado en Firestore.
 */
public class UserItem {
    private final String uid;
    private final String firstName;
    private final String lastName;
    private final String email;

    public UserItem(String uid, String firstName, String lastName, String email) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public String getUid()       { return uid; }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
    public String getEmail()     { return email; }

    /**
     * Devuelve el texto completo (para filtrar): nombre + apellidos + email, en minúsculas
     */
    public String getSearchableText() {
        return (firstName + " " + lastName + " " + email).toLowerCase();
    }
}