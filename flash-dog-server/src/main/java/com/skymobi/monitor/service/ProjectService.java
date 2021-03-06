/**
 * Copyright (C) 2012 skymobi LTD
 *
 * Licensed under GNU GENERAL PUBLIC LICENSE  Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/gpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.skymobi.monitor.service;

import com.skymobi.monitor.model.Project;
import com.skymobi.monitor.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Resource;
import java.util.List;

/**
 * Author: Hill.Hu
 * Email:  hill.hu@sky-mobi.com
 * Date: 11-11-30 下午6:37
 */
@SuppressWarnings("unchecked")
public class ProjectService {
    private static Logger logger = LoggerFactory.getLogger(ProjectService.class);

    @Resource
    private AlertService alertService;
    @Resource
    private MongoTemplate mongoTemplate;
    private String collectionName;
    @Resource
    private TaskService taskService;


    public List<Project> findProjects() {
        List<Project> projects = mongoTemplate.
                find(new Query(), Project.class, collectionName);
        return projects;
    }

    public void saveProject(Project project) {

        logger.debug("project has be save {}", project);
        mongoTemplate.save(project, collectionName);

    }

    public Project findProject(String projectName) {


        return mongoTemplate.findOne(new Query(Criteria.where("name").is(projectName)),
                Project.class, collectionName);
    }

    public void init() {
        List<Project> projects = findProjects();
        for (Project project : projects) {
            taskService.startTasks(project);
        }

    }


    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }


    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public Project saveTask(String projectName, Task task) {
        Project project = findProject(projectName);
        project.saveTask(task);
        taskService.scheduledTask(project, task);
        saveProject(project);

        return project;
    }

    public void removeTask(String projectName, String taskName) {
        Project project = findProject(projectName);
        Task task = project.removeTask(taskName);
        saveProject(project);
        taskService.removeScheduled(projectName, task);
    }


    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }

    public void remove(String projectName) {
        Project project = findProject(projectName);
        if (project == null) {
            logger.warn(" project  [{}] not exist", projectName);
            return;
        }
        for (Task task : project.getTasks()) {
            removeTask(projectName, task.getName());
        }
        mongoTemplate.remove(project,
                collectionName);
        logger.debug("remove project by name={} success", projectName);
    }

}
