package lv.kid.vermut.intellij.yaml.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lv.kid.vermut.intellij.yaml.VaultWrapper;
import lv.kid.vermut.intellij.yaml.psi.NeonFile;
import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;

public class VaultBaseAction extends AnAction{

    public void performAction(VaultWrapper vaultWrapper, String path, String password) throws InterruptedException, IOException {
        throw new NotImplementedException();
    }

    @Override
    public void update(AnActionEvent e) {
        PsiElement data = e.getData((LangDataKeys.PSI_ELEMENT));
        e.getPresentation().setVisible(data instanceof PsiFile);
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

        String password = Messages.showInputDialog("Vault password", "Pealkiri", null);
        PsiElement data = anActionEvent.getData((LangDataKeys.PSI_ELEMENT));
        String absPath = ((NeonFile) data).getVirtualFile().getCanonicalPath();
        try {
            VaultWrapper vaultWrapper = VaultWrapper.fromPath();
            performAction(vaultWrapper, absPath, password);
        } catch (IOException e) {
            Messages.showErrorDialog(e.getMessage(), "Error!");
        } catch (InterruptedException e) {
            Messages.showErrorDialog(e.getMessage(), "Error!");
        }

        final Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

        for (VirtualFile virtualFile: fileEditorManager.getOpenFiles()) {
            if (virtualFile.getCanonicalPath().equals(absPath)) {
                virtualFile.refresh(true, true);
            } else {
                System.out.println(absPath + " != " + virtualFile.getCanonicalPath());
            }
        }
    }
}
