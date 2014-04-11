package org.obiba.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.util.FS;

public final class GitUtils {

  public static final String HEAD_COMMIT_ID = "HEAD";

  private GitUtils() {}

  public Repository getRepository(File repoFile) {
    if(!repoFile.exists()) {
      throw new NoSuchGitRepositoryException(repoFile.getAbsolutePath());
    }
    try {
      return new FileRepositoryBuilder().setGitDir(repoFile).readEnvironment().findGitDir().build();
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getNthCommitId(String commitId, int nth) {
    return commitId + "~" + nth;
  }

  /**
   * Clone or Fetch a repository. If the local repository does not exist,
   * clone is called. If the repository does exist, fetch is called. By
   * default the clone/fetch retrieves the remote heads, tags, and notes.
   *
   * @param repositoriesFolder
   * @param name
   * @param fromUrl
   * @throws Exception
   */
  public static void cloneRepository(File repositoriesFolder, String name, String fromUrl)
      throws IOException, GitAPIException {
    String repoName = name;
    // normal repository, strip .git suffix
    if(name.toLowerCase().endsWith(Constants.DOT_GIT_EXT)) {
      repoName = name.substring(0, name.indexOf(Constants.DOT_GIT_EXT));
    }

    File folder = new File(repositoriesFolder, repoName);
    if(folder.exists()) {
      File gitDir = RepositoryCache.FileKey.resolve(new File(repositoriesFolder, repoName), FS.DETECTED);
      Repository repository = new FileRepositoryBuilder().setGitDir(gitDir).build();
      fetchRepository(repository);
      repository.close();
    } else {
      CloneCommand clone = new CloneCommand();
      clone.setBare(false);
      clone.setCloneAllBranches(true);
      clone.setURI(fromUrl);
      clone.setDirectory(folder);
      Repository repository = clone.call().getRepository();

      // Now we have to fetch because CloneCommand doesn't fetch refs/notes nor does it allow manual RefSpec.
      fetchRepository(repository);
      repository.close();
    }
  }

  /**
   * Fetch updates from the remote repository. If refSpecs is unspecified,
   * remote heads, tags, and notes are retrieved.
   *
   * @param credentialsProvider
   * @param repository
   * @param refSpecs
   * @return FetchResult
   * @throws Exception
   */
  public static FetchResult fetchRepository(Repository repository, RefSpec... refSpecs) throws GitAPIException {
    Git git = new Git(repository);
    List<RefSpec> specs = new ArrayList<>();
    if(refSpecs == null || refSpecs.length == 0) {
      specs.add(new RefSpec("+refs/heads/*:refs/remotes/origin/*"));
      specs.add(new RefSpec("+refs/tags/*:refs/tags/*"));
      specs.add(new RefSpec("+refs/notes/*:refs/notes/*"));
    } else {
      specs.addAll(Arrays.asList(refSpecs));
    }
    FetchCommand fetch = git.fetch();
    fetch.setRefSpecs(specs);
    return fetch.call();
  }
}