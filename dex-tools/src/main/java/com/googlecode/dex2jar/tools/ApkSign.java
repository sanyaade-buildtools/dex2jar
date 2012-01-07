package com.googlecode.dex2jar.tools;

import java.io.File;
import java.lang.reflect.Method;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class ApkSign extends BaseCmd {
    public static void main(String[] args) {
        new ApkSign().doMain(args);
    }

    @Opt(opt = "f", longOpt = "force", hasArg = false, description = "force overwrite")
    private boolean forceOverwrite = false;
    @Opt(opt = "o", longOpt = "output", description = "output .apk file, default is $current_dir/[apk-name]-signed.apk", argName = "out-apk-file")
    private File output;
    @Opt(opt = "w", longOpt = "sign-whole", hasArg = false, description = "Sign whole apk file")
    private boolean signWhole = false;

    public ApkSign() {
        super("d2j-apk-sign <apk>", "Sign an android apk file use a test certificate.");
    }

    @Override
    protected void doCommandLine() throws Exception {
        if (remainingArgs.length != 1) {
            usage();
            return;
        }

        File apkIn = new File(remainingArgs[0]);
        if (!apkIn.exists() || !apkIn.isFile()) {
            System.err.println(apkIn + " is not exists");
            usage();
            return;
        }

        if (output == null) {
            output = new File(FilenameUtils.getBaseName(apkIn.getName()) + "-signed.apk");
        }

        if (output.exists() && !forceOverwrite) {
            System.err.println(output + " exists, use --force to overwrite");
            usage();
            return;
        }

        // TODO check sun JVM

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(ApkSign.class
                .getResourceAsStream("ApkSign.cer"));
        KeyFactory rSAKeyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = rSAKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(IOUtils.toByteArray(ApkSign.class
                .getResourceAsStream("ApkSign.private"))));

        Class<?> clz = Class.forName("com.android.signapk.SignApk");
        Method m = clz
                .getMethod("sign", X509Certificate.class, PrivateKey.class, boolean.class, File.class, File.class);
        m.setAccessible(true);

        System.out.println("sign " + apkIn + " -> " + output);
        m.invoke(null, cert, privateKey, this.signWhole, apkIn, output);
    }
}
