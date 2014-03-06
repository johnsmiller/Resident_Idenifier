package com.sjsurha.resident_identifier;

//Code modified from http://stackoverflow.com/questions/6262374/encryption-with-aes-algorithm-in-java
//Used to encrypt/decrypt model class objects
//THIS CODE HAS BEEN MODIFIED FROM ITS ORIGINAL OWNER. PROPER AUTHOR INFORMAION WILL BE PROVIDED ONCE AVAILABLE

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Edited by John
 */
public final class SealObject {
    private final byte[] salt = "a9v5n39s".getBytes();
    private Cipher cipher;
    private Cipher dcipher;

    /**
     *
     * @param Password
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     */
    public SealObject(String Password) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException
    {
        char[] password = Password.toCharArray();
        // Create key
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password, salt, 1024, 128);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        // Init ciphers
        cipher = Cipher.getInstance("AES");
        dcipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        dcipher.init(Cipher.DECRYPT_MODE, secret);
        
    }
    
    /**
     *
     * @param input
     * @return
     * @throws IOException
     * @throws IllegalBlockSizeException
     */
    public SealedObject encrypt(Model input) throws IOException, IllegalBlockSizeException
    {
        return new SealedObject(input, cipher);
    }
    
    /**
     *
     * @param input
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public Model decrypt(SealedObject input) throws IOException, ClassNotFoundException, IllegalBlockSizeException, BadPaddingException
    {
        return (Model) input.getObject(dcipher);
    }
    
}
