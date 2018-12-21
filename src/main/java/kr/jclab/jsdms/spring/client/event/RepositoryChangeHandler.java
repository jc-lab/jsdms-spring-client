package kr.jclab.jsdms.spring.client.event;

import java.io.File;

public interface RepositoryChangeHandler {
    void onRepositoryChanged(String targetName, String branchName, File branchDir);
}
