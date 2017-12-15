package lv.kid.vermut.intellij.yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

import static java.lang.Math.max;
import static org.apache.logging.log4j.core.util.Charsets.UTF_8;

public class VaultWrapper {

    private static final String ANSIBLE_EXECUTABLE_PATH = "ansible-vault";
    private static final String ANSIBLE_ACTION_ENCRYPT = "encrypt";
    private static final String ANSIBLE_ACTION_DECRYPT = "decrypt";

    public VaultWrapper(String ansibleVaultPath) {
        this.ansibleVaultPath = ansibleVaultPath;
    }

    private String ansibleVaultPath;

    public static VaultWrapper fromPath() throws IOException {
        String path = getAnsibleVaultPath();
        if (path == null) {
            throw new IOException("Could not find " + ANSIBLE_EXECUTABLE_PATH + " in your PATH, check your installation!");
        }
        return new VaultWrapper(path);

    }

    private File dumpToTempFile(String password) throws IOException {
        File tempFile = File.createTempFile("intellij-ansible", "");
        System.out.printf("temp file path: " + tempFile.getAbsolutePath());
        OutputStream os = new FileOutputStream(tempFile.getAbsoluteFile());
        os.write(password.getBytes());
        os.close();
        return tempFile;
    }

    private static String getAnsibleVaultPath() throws IOException {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            throw new IOException("PATH environment varible is undefined");
        }
        String[] parts = pathEnv.split(":");
        for (String part: parts) {
            File pathCandidate = new File(part, ANSIBLE_EXECUTABLE_PATH);
            if (pathCandidate.exists()) {
                return pathCandidate.toString();
            }
        }
        return null;
    }

    private void executeVaultCommand(String... args) throws IOException, InterruptedException {
        ArrayList<String> arguments = new ArrayList<String>();
        arguments.add(ansibleVaultPath);
        Collections.addAll(arguments, args);
        ProcessBuilder pb = new ProcessBuilder(
                arguments
        );
        pb.redirectErrorStream(true);

        Process p = pb.start();
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append("\n");
        }
        p.waitFor();
        stringBuilder.setLength(max(stringBuilder.length() - 1, 0));
        if (p.exitValue() != 0) {
            throw new IOException("Program exited with non-zero exit code: \n" + stringBuilder.toString());
        }
    }

    private String getAction(boolean encrypt) {
        return encrypt ? ANSIBLE_ACTION_ENCRYPT: ANSIBLE_ACTION_DECRYPT;
    }

    private void cryptFile(String contentsPath, String key, boolean encrypt) throws IOException, InterruptedException {
        File tmpFile = null;

        try {
            tmpFile = dumpToTempFile(key);
            executeVaultCommand(getAction(encrypt), "--vault-password-file=" + tmpFile.getAbsolutePath(), contentsPath);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    private String convertFileToString(String path) throws IOException {
        FileInputStream stream = new FileInputStream(path);
        try {
            Reader reader = new BufferedReader(new InputStreamReader(stream, UTF_8));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } finally {
            stream.close();
        }
    }

    private String cryptString(String text, String key, boolean encrypt) throws IOException, InterruptedException {
        File keyFile = null;
        File contentsFile = null;
        try {
            keyFile = dumpToTempFile(key);
            contentsFile = dumpToTempFile(text);
            cryptFile(contentsFile.getAbsolutePath(), keyFile.getAbsolutePath(), encrypt);
            return convertFileToString(contentsFile.getPath());

        } finally {
            if (keyFile != null) {
                keyFile.delete();
            }
            if (contentsFile != null) {
                contentsFile.delete();
            }
        }
    }

    public String encryptString(String clearText, String encryptionKey) throws IOException, InterruptedException {
        return cryptString(clearText, encryptionKey, true);
    }

    public String decryptString(String cipherText, String encryptionKey) throws IOException, InterruptedException {
        return cryptString(cipherText, encryptionKey,false);
    }

    public void encryptFile(String encryptedFile, String encryptionKey) throws IOException, InterruptedException {
        cryptFile(encryptedFile, encryptionKey, true);
    }

    public void decryptFile(String decryptedFile, String encryptionKey) throws IOException, InterruptedException {
        cryptFile(decryptedFile, encryptionKey, false);
    }

}

