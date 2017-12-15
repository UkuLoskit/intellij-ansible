package lv.kid.vermut.intellij.yaml.actions;

import lv.kid.vermut.intellij.yaml.VaultWrapper;

import java.io.IOException;

public class FileEncryptionAction extends VaultBaseAction {

    @Override
    public void performAction(VaultWrapper vaultWrapper, String path, String password) throws InterruptedException, IOException {
        vaultWrapper.encryptFile(path, password);
    }
}
