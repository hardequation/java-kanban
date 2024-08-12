package com.kanban;

import com.kanban.controllers.InMemoryTaskManager;
import com.kanban.controllers.Managers;
import org.junit.jupiter.api.BeforeEach;

class InMemoryTaskManagerTest extends TaskManagerTest<InMemoryTaskManager> {

    @BeforeEach
    public void setup() {
        super.setup();
        historyManager = Managers.getDefaultHistory();
        taskManager = new InMemoryTaskManager(historyManager);
    }
}
