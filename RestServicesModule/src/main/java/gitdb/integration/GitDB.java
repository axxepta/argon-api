package gitdb.integration;

/*
 * Copyright 2017-2018 E257.FI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Some parts of this code are based on examples of jgit and gerrit.
 *
 * jgit: (how to commit code)
 * by: Copyright (C) 2010-2012, Christian Halstrick <christian.halstrick@sap.com>
 *     and other copyright owners as documented in the project's IP log.
 * license: Eclipse Distribution License v1.0
 * url: https://github.com/eclipse/jgit
 * path: org.eclipse.jgit/src/org/eclipse/jgit/api/CommitCommand.java
 * commit: 3b4448637fbb9d74e0c9d44048ba76bb7c1214ce
 *
 * gerrit: (how to save/write file with bare repository)
 * by: Copyright (C) 2010 The Android Open Source Project
 * license: Apache License, version 2.0
 * url: https://github.com/GerritCodeReview/gerrit
 * path: gerrit-server/src/main/java/com/google/gerrit/server/git/VersionedMetaData.java
 * commit: b4af8cad4d3982a0bba763a5e681d26078da5a0e
 */

import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.file.LockFile;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

public class GitDB implements Closeable {

    private int writeLockWait = 25;
    protected Repository repo;
    protected DirCache index;
    private LockFile repoLock;

    protected String getRefName() {
        return "refs/heads/master";
    }

    protected File getRefLockFile() {
        return Paths.get(repo.getDirectory().toString(),"tackler/locks", getRefName()).toFile();
    }
    protected File getWriteLockFile() {
        return Paths.get(getRefLockFile().toString(), "write").toFile();
    }

    public GitDB(Repository repo) throws IOException, InterruptedException {
        this.repo = repo;
        repoLock = new LockFile(getRefLockFile());
        acquireLock(repoLock, 2000, 25);
        index = dircache();
    }

    @Override
    public void close() throws IOException {
        if (repoLock != null) {
            repoLock.unlock();
        }
    }

    private DirCache dircache() throws IOException {
        try (RevWalk rw = new RevWalk(repo)) {
            Ref head = repo.exactRef(getRefName());

            RevCommit revision = head != null ? (head.getObjectId() != null ? rw.parseCommit(head.getObjectId()) : null) : null;
            RevTree tree = revision != null ? rw.parseTree(revision) : null;

            return readTreeToIndex(tree);
        }
    }

    private static void acquireLock(LockFile lock, int maxWait, int sleep) throws IOException, InterruptedException {
        int wait=0;
        while (!lock.lock()) {
            // TODO exception to freedom if too many rounds
            Thread.sleep(sleep);
            wait += sleep;
            if (maxWait < wait) {
                throw new RuntimeException("Can not get lock: " + lock.toString());
            }
        }
    }

    public RevCommit commit(DataItem data) throws UnmergedPathsException, InterruptedException, IOException, WrongRepositoryStateException, NoHeadException, ConcurrentRefUpdateException {
        LockFile writeLock = new LockFile(getWriteLockFile());
        try {
            acquireLock(writeLock, 2000, writeLockWait);
            return commit22(data);
        } finally {
            writeLock.unlock();
        }
    }

    // JGIT-porcelain
    private RevCommit commit22(DataItem data) throws IOException, WrongRepositoryStateException, NoHeadException, ConcurrentRefUpdateException, UnmergedPathsException, InterruptedException {
        List<ObjectId> parents = new LinkedList<>();

        try (RevWalk rw = new RevWalk(repo)) {
            Ref head = repo.exactRef(getRefName());

            // determine the current head of ref and the commit it is referring to
            ObjectId headId = repo.resolve(getRefName() + "^{commit}");

            if (headId != null) {
                parents.add(headId);
            }

            RevCommit revision = head != null ? (head.getObjectId() != null ? rw.parseCommit(head.getObjectId()) : null) : null;
            RevTree tree = revision != null ? rw.parseTree(revision) : null;

            index = readTreeToIndex(tree);

            try (ObjectInserter odi = repo.newObjectInserter()) {
                // write data at path to the index
                saveUTF8(index, odi, data.getPath(), data.getData());

                // Write the index as tree to the object database. This may
                // fail for example when the index contains unmerged paths
                // (unresolved conflicts)
                ObjectId indexTreeId = index.writeTree(odi);

                // Check for empty commits
                if (headId != null) {
                    RevCommit headCommit = rw.parseCommit(headId);
                    headCommit.getTree();
                    if (indexTreeId.equals(headCommit.getTree())) {
                        // TODO log empty commit and return?
                    }
                }

                // Create a Commit object, populate it and write it
                CommitBuilder commit = new CommitBuilder();
                PersonIdent pi = new PersonIdent("name", "email");

                commit.setAuthor(pi);
                commit.setCommitter(pi);
                commit.setMessage("db commit by v2");

                commit.setParentIds(parents);
                commit.setTreeId(indexTreeId);

                ObjectId commitId = odi.insert(commit);
                odi.flush();

                RevCommit revCommit = rw.parseCommit(commitId);
                RefUpdate ru = repo.updateRef(Constants.HEAD);

                ru.setNewObjectId(commitId);
                ru.setRefLogIdent(pi);
                ru.setRefLogMessage("reflog commit v2", true);

                if (headId != null)
                    ru.setExpectedOldObjectId(headId);
                else
                    ru.setExpectedOldObjectId(ObjectId.zeroId());

                RefUpdate.Result rc = ru.forceUpdate();
                switch (rc) {
                    case NEW:
                    case FORCED:
                    case FAST_FORWARD: {
                        return revCommit;
                    }
                    case REJECTED:
                    case LOCK_FAILURE:
                        throw new ConcurrentRefUpdateException(
                                "Concurrent ref error: Could not lock: ", ru.getRef(), rc);
                    default:
                        throw new JGitInternalException(MessageFormat.format(
                                JGitText.get().updatingRefFailed, Constants.HEAD,
                                commitId.toString(), rc));
                }
            }
        }
    }


    /**
     * Read existing tree to new index. Index is newInCore based.
     */
    private DirCache readTreeToIndex(RevTree tree) throws IOException {
        if (index != null) {
            return index;
        } else {
            if (tree != null) {
                try (ObjectReader reader = repo.newObjectReader()) {
                    return DirCache.read(reader, tree);
                }
            } else {
                return DirCache.newInCore();
            }
        }
    }

    static protected void saveUTF8(DirCache index, ObjectInserter odi, String path, String text) throws IOException {
        saveData(index, odi, path, Constants.encode(text));
    }

    static private void saveData(DirCache index, ObjectInserter odi, String path, byte[] blob) throws IOException {
        assert blob != null;
        assert 0 < blob.length;

        ObjectId blobId = odi.insert(Constants.OBJ_BLOB, blob);

        DirCacheEditor editor = index.editor();
        editor.add(
                new DirCacheEditor.PathEdit(path) {
                    @Override
                    public void apply(DirCacheEntry entry) {
                        entry.setFileMode(FileMode.REGULAR_FILE);
                        entry.setObjectId(blobId);
                    }
                });
        editor.finish();
    }
}
