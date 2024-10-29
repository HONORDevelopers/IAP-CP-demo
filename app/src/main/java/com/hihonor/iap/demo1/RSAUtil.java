package com.hihonor.iap.demo1;

import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtil {

    /**
     * 获取公钥
     *
     * @param publicKey 公钥字符串
     * @return
     */
    public static PublicKey getPublicKey(String publicKey) {
        PublicKey mPpublicKey = null;
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] decodedKey = Base64.decode(publicKey.getBytes(), Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            mPpublicKey = keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            Log.e("TAG", "Exception: ", e);
        }
        return mPpublicKey;
    }

    /**
     * 验签
     *
     * @param srcData   原始字符串
     * @param publicKey 公钥
     * @param sign      签名
     * @return 是否验签通过
     */
    public static boolean verify(String srcData, PublicKey publicKey, String sign) {
        boolean verify = false;
        try {
            byte[] keyBytes = publicKey.getEncoded();
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey key = keyFactory.generatePublic(keySpec);
            // 7.0.3.000SDK版本之后,客户端签名方式由“SHA256withRSA”改为“SHA256withRSA/PSS”
            Signature signature = Signature.getInstance("SHA256withRSA/PSS");
            signature.initVerify(key);
            signature.update(("" + srcData).getBytes());
            verify = signature.verify(Base64.decode(sign.getBytes(), Base64.DEFAULT));
        } catch (Exception e) {
            Log.e("TAG", "Exception: ", e);
        }
        return verify;
    }

}
