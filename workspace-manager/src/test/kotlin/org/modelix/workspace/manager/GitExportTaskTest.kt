package org.modelix.workspace.manager

import org.modelix.services.gitconnector.GitExportTask
import kotlin.test.Test
import kotlin.test.assertTrue

class GitExportTaskTest {

    @Test
    fun `timestamp format`() {
        assertTrue(GitExportTask.timeForBranchName().matches(Regex("""\d{8}-\d{6}""")))
    }
}
