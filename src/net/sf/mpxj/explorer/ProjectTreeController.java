/*
 * file:       ProjectTreeController.java
 * author:     Jon Iles
 * copyright:  (c) Packwood Software 2014
 * date:       06/07/2014
 */

/*
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.mpxj.explorer;

import java.io.File;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sf.mpxj.Group;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Resource;
import net.sf.mpxj.Task;
import net.sf.mpxj.TaskContainer;
import net.sf.mpxj.reader.ProjectReader;
import net.sf.mpxj.reader.ProjectReaderUtility;

/**
 * Implements the controller component of the ProjectTree MVC.
 */
public class ProjectTreeController
{
   private final ProjectTreeModel m_model;

   /**
    * Constructor.
    * 
    * @param model PoiTree model
    */
   public ProjectTreeController(ProjectTreeModel model)
   {
      m_model = model;
   }

   /**
    * Command to load a file.
    * 
    * @param file file to load
    */
   public void loadFile(File file)
   {
      ProjectFile projectFile;

      try
      {
         ProjectReader reader = ProjectReaderUtility.getProjectReader(file.getName());
         projectFile = reader.read(file);
      }

      catch (Exception ex)
      {
         throw new RuntimeException(ex);
      }

      DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(projectFile);

      DefaultMutableTreeNode headerNode = new DefaultMutableTreeNode(projectFile.getProjectHeader())
      {
         @Override public String toString()
         {
            return "Header";
         }
      };

      projectNode.add(headerNode);

      DefaultMutableTreeNode tasksFolder = new DefaultMutableTreeNode("Tasks");
      projectNode.add(tasksFolder);
      addTasks(tasksFolder, projectFile);

      DefaultMutableTreeNode resourcesFolder = new DefaultMutableTreeNode("Resources");
      projectNode.add(resourcesFolder);
      addResources(resourcesFolder, projectFile);

      DefaultMutableTreeNode assignmentsFolder = new DefaultMutableTreeNode("Assignments");
      projectNode.add(assignmentsFolder);

      DefaultMutableTreeNode calendarsFolder = new DefaultMutableTreeNode("Calendars");
      projectNode.add(calendarsFolder);

      DefaultMutableTreeNode groupsFolder = new DefaultMutableTreeNode("Groups");
      projectNode.add(groupsFolder);
      addGroups(groupsFolder, projectFile);

      m_model.setRoot(projectNode);
   }

   /**
    * Add tasks to the tree.
    * 
    * @param parentNode parent tree node
    * @param parent parent task container
    */
   private void addTasks(DefaultMutableTreeNode parentNode, TaskContainer parent)
   {
      for (Task task : parent.getChildTasks())
      {
         final Task t = task;
         DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(task)
         {
            @Override public String toString()
            {
               return t.getName();
            }
         };
         parentNode.add(childNode);
         addTasks(childNode, task);
      }
   }

   /**
    * Add resources to the tree.
    * 
    * @param parentNode parent tree node
    * @param file resource container
    */
   private void addResources(DefaultMutableTreeNode parentNode, ProjectFile file)
   {
      for (Resource resource : file.getAllResources())
      {
         final Resource r = resource;
         DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(resource)
         {
            @Override public String toString()
            {
               return r.getName();
            }
         };
         parentNode.add(childNode);
      }
   }

   /**
    * Add groups to the tree.
    * 
    * @param parentNode parent tree node
    * @param file group container
    */
   private void addGroups(DefaultMutableTreeNode parentNode, ProjectFile file)
   {
      for (Group group : file.getAllGroups())
      {
         final Group g = group;
         DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(group)
         {
            @Override public String toString()
            {
               return g.getName();
            }
         };
         parentNode.add(childNode);
      }
   }

}
