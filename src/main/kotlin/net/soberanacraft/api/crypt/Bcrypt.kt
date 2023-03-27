package net.soberanacraft.api.crypt

import at.favre.lib.crypto.bcrypt.BCrypt

fun verify(password: String, hash: String) : Boolean {
    return try {
        val array = password.toCharArray()
        BCrypt.verifyer().verify(array, hash).verified
    } catch (e: Error) {
        false
    }
}

fun hash(password: String) : String? {
    return try {
        BCrypt.withDefaults().hashToString(12, password.toCharArray())
    } catch (e: Error) {
        null
    }
}