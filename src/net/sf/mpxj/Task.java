/*
 * file:       Task.java
 * author:     Scott Melville
 *             Jon Iles
 * copyright:  (c) Packwood Software 2002-2003
 * date:       15/08/2002
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

package net.sf.mpxj;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import net.sf.mpxj.listener.FieldListener;
import net.sf.mpxj.utility.BooleanUtility;
import net.sf.mpxj.utility.DateUtility;
import net.sf.mpxj.utility.NumberUtility;

/**
 * This class represents a task record from an project file.
 */
public final class Task extends ProjectEntity implements Comparable<Task>, FieldContainer
{
   /**
    * Default constructor.
    *
    * @param file Parent file to which this record belongs.
    * @param parent Parent task
    */
   Task(ProjectFile file, Task parent)
   {
      super(file);

      setType(TaskType.FIXED_UNITS);
      setConstraintType(ConstraintType.AS_SOON_AS_POSSIBLE);
      setTaskMode(TaskMode.AUTO_SCHEDULED);
      setActive(true);

      m_parent = parent;

      if (file.getAutoTaskUniqueID() == true)
      {
         setUniqueID(Integer.valueOf(file.getTaskUniqueID()));
      }

      if (file.getAutoTaskID() == true)
      {
         setID(Integer.valueOf(file.getTaskID()));
      }

      if (file.getAutoWBS() == true)
      {
         generateWBS(parent);
      }

      if (file.getAutoOutlineNumber() == true)
      {
         generateOutlineNumber(parent);
      }

      if (file.getAutoOutlineLevel() == true)
      {
         if (parent == null)
         {
            setOutlineLevel(Integer.valueOf(1));
         }
         else
         {
            setOutlineLevel(Integer.valueOf(NumberUtility.getInt(parent.getOutlineLevel()) + 1));
         }
      }
   }

   /**
    * This method is used to automatically generate a value
    * for the WBS field of this task.
    *
    * @param parent Parent Task
    */
   public void generateWBS(Task parent)
   {
      String wbs;

      if (parent == null)
      {
         if (NumberUtility.getInt(getUniqueID()) == 0)
         {
            wbs = "0";
         }
         else
         {
            wbs = Integer.toString(getParentFile().getChildTaskCount() + 1);
         }
      }
      else
      {
         wbs = parent.getWBS();

         int index = wbs.lastIndexOf(".0");

         if (index != -1)
         {
            wbs = wbs.substring(0, index);
         }

         if (wbs.equals("0") == true)
         {
            wbs = Integer.toString(parent.getChildTaskCount() + 1);
         }
         else
         {
            wbs += ("." + (parent.getChildTaskCount() + 1));
         }
      }

      setWBS(wbs);
   }

   /**
    * This method is used to automatically generate a value
    * for the Outline Number field of this task.
    *
    * @param parent Parent Task
    */
   public void generateOutlineNumber(Task parent)
   {
      String outline;

      if (parent == null)
      {
         if (NumberUtility.getInt(getUniqueID()) == 0)
         {
            outline = "0";
         }
         else
         {
            outline = Integer.toString(getParentFile().getChildTaskCount() + 1);
         }
      }
      else
      {
         outline = parent.getOutlineNumber();

         int index = outline.lastIndexOf(".0");

         if (index != -1)
         {
            outline = outline.substring(0, index);
         }

         if (outline.equals("0") == true)
         {
            outline = Integer.toString(parent.getChildTaskCount() + 1);
         }
         else
         {
            outline += ("." + (parent.getChildTaskCount() + 1));
         }
      }

      setOutlineNumber(outline);
   }

   /**
    * This method is used to add notes to the current task.
    *
    * @param notes notes to be added
    */
   public void setNotes(String notes)
   {
      set(TaskField.NOTES, notes);
   }

   /**
    * This method allows nested tasks to be added, with the WBS being
    * completed automatically.
    *
    * @return new task
    */
   public Task addTask()
   {
      ProjectFile parent = getParentFile();

      Task task = new Task(parent, this);

      m_children.add(task);

      parent.addTask(task);

      setSummary(true);

      return (task);
   }

   /**
    * This method is used to associate a child task with the current
    * task instance. It has package access, and has been designed to
    * allow the hierarchical outline structure of tasks in an MPX
    * file to be constructed as the file is read in.
    *
    * @param child Child task.
    * @param childOutlineLevel Outline level of the child task.
    */
   public void addChildTask(Task child, int childOutlineLevel)
   {
      int outlineLevel = NumberUtility.getInt(getOutlineLevel());

      if ((outlineLevel + 1) == childOutlineLevel)
      {
         m_children.add(child);
         setSummary(true);
      }
      else
      {
         if (m_children.isEmpty() == false)
         {
            (m_children.get(m_children.size() - 1)).addChildTask(child, childOutlineLevel);
         }
      }
   }

   /**
    * This method is used to associate a child task with the current
    * task instance. It has been designed to
    * allow the hierarchical outline structure of tasks in an MPX
    * file to be updated once all of the task data has been read.
    *
    * @param child child task
    */
   public void addChildTask(Task child)
   {
      child.m_parent = this;
      m_children.add(child);
      setSummary(true);

      if (getParentFile().getAutoOutlineLevel() == true)
      {
         child.setOutlineLevel(Integer.valueOf(NumberUtility.getInt(getOutlineLevel()) + 1));
      }
   }

   /**
    * Removes a child task.
    *
    * @param child child task instance
    */
   public void removeChildTask(Task child)
   {
      if (m_children.remove(child))
      {
         child.m_parent = null;
      }
      setSummary(!m_children.isEmpty());
   }

   /**
    * This method allows the list of child tasks to be cleared in preparation
    * for the hierarchical task structure to be built.
    */
   public void clearChildTasks()
   {
      m_children.clear();
      setSummary(false);
   }

   /**
    * This method allows recurring task details to be added to the
    * current task.
    *
    * @return RecurringTask object
    */
   public RecurringTask addRecurringTask()
   {
      if (m_recurringTask == null)
      {
         m_recurringTask = new RecurringTask();
      }

      return (m_recurringTask);
   }

   /**
    * This method retrieves the recurring task record. If the current
    * task is not a recurring task, then this method will return null.
    *
    * @return Recurring task record.
    */
   public RecurringTask getRecurringTask()
   {
      return (m_recurringTask);
   }

   /**
    * This method allows a resource assignment to be added to the
    * current task.
    *
    * @param resource the resource to assign
    * @return ResourceAssignment object
    */
   public ResourceAssignment addResourceAssignment(Resource resource)
   {
      ResourceAssignment assignment = getExistingResourceAssignment(resource);

      if (assignment == null)
      {
         assignment = new ResourceAssignment(getParentFile(), this);
         m_assignments.add(assignment);
         getParentFile().addResourceAssignment(assignment);

         assignment.setTaskUniqueID(getUniqueID());
         assignment.setWork(getDuration());
         assignment.setUnits(ResourceAssignment.DEFAULT_UNITS);

         if (resource != null)
         {
            assignment.setResourceUniqueID(resource.getUniqueID());
            resource.addResourceAssignment(assignment);
         }
      }

      return (assignment);
   }

   /**
    * Add a resource assignment which has been populated elsewhere.
    * 
    * @param assignment resource assignment
    */
   public void addResourceAssignment(ResourceAssignment assignment)
   {
      if (getExistingResourceAssignment(assignment.getResource()) == null)
      {
         m_assignments.add(assignment);
         getParentFile().addResourceAssignment(assignment);

         Resource resource = assignment.getResource();
         if (resource != null)
         {
            resource.addResourceAssignment(assignment);
         }
      }
   }

   /**
    * Retrieves an existing resource assignment if one is present,
    * to prevent duplicate resource assignments being added.
    * 
    * @param resource resource to test for
    * @return existing resource assignment
    */
   private ResourceAssignment getExistingResourceAssignment(Resource resource)
   {
      ResourceAssignment assignment = null;
      Integer resourceUniqueID = null;

      if (resource != null)
      {
         Iterator<ResourceAssignment> iter = m_assignments.iterator();
         resourceUniqueID = resource.getUniqueID();

         while (iter.hasNext() == true)
         {
            assignment = iter.next();
            Integer uniqueID = assignment.getResourceUniqueID();
            if (uniqueID != null && uniqueID.equals(resourceUniqueID) == true)
            {
               break;
            }
            assignment = null;
         }
      }

      return assignment;
   }

   /**
    * This method allows the list of resource assignments for this
    * task to be retrieved.
    *
    * @return list of resource assignments
    */
   public List<ResourceAssignment> getResourceAssignments()
   {
      return (m_assignments);
   }

   /**
    * Internal method used as part of the process of removing a
    * resource assignment.
    *
    * @param assignment resource assignment to be removed
    */
   void removeResourceAssignment(ResourceAssignment assignment)
   {
      m_assignments.remove(assignment);
   }

   /**
    * This method allows a predecessor relationship to be added to this
    * task instance.
    *
    * @param targetTask the predecessor task
    * @param type relation type
    * @param lag relation lag
    * @return relationship
    */
   @SuppressWarnings("unchecked") public Relation addPredecessor(Task targetTask, RelationType type, Duration lag)
   {
      //
      // Ensure that we have a valid lag duration
      //
      if (lag == null)
      {
         lag = Duration.getInstance(0, TimeUnit.DAYS);
      }

      //
      // Retrieve the list of predecessors
      //
      List<Relation> predecessorList = (List<Relation>) getCachedValue(TaskField.PREDECESSORS);
      if (predecessorList == null)
      {
         predecessorList = new LinkedList<Relation>();
         set(TaskField.PREDECESSORS, predecessorList);
      }

      //
      // Ensure that there is only one predecessor relationship between
      // these two tasks.
      //
      Relation predecessorRelation = null;
      Iterator<Relation> iter = predecessorList.iterator();
      while (iter.hasNext() == true)
      {
         predecessorRelation = iter.next();
         if (predecessorRelation.getTargetTask() == targetTask)
         {
            if (predecessorRelation.getType() != type || predecessorRelation.getLag().compareTo(lag) != 0)
            {
               predecessorRelation = null;
            }
            break;
         }
         predecessorRelation = null;
      }

      //
      // If necessary, create a new predecessor relationship
      //
      if (predecessorRelation == null)
      {
         predecessorRelation = new Relation(this, targetTask, type, lag);
         predecessorList.add(predecessorRelation);
      }

      //
      // Retrieve the list of successors
      //
      List<Relation> successorList = (List<Relation>) targetTask.getCachedValue(TaskField.SUCCESSORS);
      if (successorList == null)
      {
         successorList = new LinkedList<Relation>();
         targetTask.set(TaskField.SUCCESSORS, successorList);
      }

      //
      // Ensure that there is only one successor relationship between
      // these two tasks.
      //
      Relation successorRelation = null;
      iter = successorList.iterator();
      while (iter.hasNext() == true)
      {
         successorRelation = iter.next();
         if (successorRelation.getTargetTask() == this)
         {
            if (successorRelation.getType() != type || successorRelation.getLag().compareTo(lag) != 0)
            {
               successorRelation = null;
            }
            break;
         }
         successorRelation = null;
      }

      //
      // If necessary, create a new successor relationship
      //
      if (successorRelation == null)
      {
         successorRelation = new Relation(targetTask, this, type, lag);
         successorList.add(successorRelation);
      }

      return (predecessorRelation);
   }

   /**
    * The % Complete field contains the current status of a task, expressed
    * as the percentage of the
    * task's duration that has been completed. You can enter percent complete,
    * or you can have
    * Microsoft Project calculate it for you based on actual duration.
    *
    * @param val value to be set
    */
   public void setPercentageComplete(Number val)
   {
      set(TaskField.PERCENT_COMPLETE, val);
   }

   /**
    * The % Work Complete field contains the current status of a task,
    * expressed as the
    * percentage of the task's work that has been completed. You can enter
    * percent work
    * complete, or you can have Microsoft Project calculate it for you
    * based on actual
    * work on the task.
    *
    * @param val value to be set
    */
   public void setPercentageWorkComplete(Number val)
   {
      set(TaskField.PERCENT_WORK_COMPLETE, val);
   }

   /**
    * The Actual Cost field shows costs incurred for work already performed
    * by all resources
    * on a task, along with any other recorded costs associated with the task.
    * You can enter
    * all the actual costs or have Microsoft Project calculate them for you.
    *
    * @param val value to be set
    */
   public void setActualCost(Number val)
   {
      set(TaskField.ACTUAL_COST, val);
   }

   /**
    * The Actual Duration field shows the span of actual working time for a
    * task so far,
    * based on the scheduled duration and current remaining work or
    * completion percentage.
    *
    * @param val value to be set
    */
   public void setActualDuration(Duration val)
   {
      set(TaskField.ACTUAL_DURATION, val);
   }

   /**
    * The Actual Finish field shows the date and time that a task actually
    * finished.
    * Microsoft Project sets the Actual Finish field to the scheduled finish
    * date if
    * the completion percentage is 100. This field contains "NA" until you
    * enter actual
    * information or set the completion percentage to 100.
    *
    * @param val value to be set
    */
   public void setActualFinish(Date val)
   {
      set(TaskField.ACTUAL_FINISH, val);
   }

   /**
    * The Actual Start field shows the date and time that a task actually began.
    * When a task is first created, the Actual Start field contains "NA." Once you
    * enter the first actual work or a completion percentage for a task, Microsoft
    * Project sets the actual start date to the scheduled start date.
    * @param val value to be set
    */
   public void setActualStart(Date val)
   {
      set(TaskField.ACTUAL_START, val);
   }

   /**
    * The Actual Work field shows the amount of work that has already been
    * done by the
    * resources assigned to a task.
    * @param val value to be set
    */
   public void setActualWork(Duration val)
   {
      set(TaskField.ACTUAL_WORK, val);
   }

   /**
    * The Baseline Cost field shows the total planned cost for a task.
    * Baseline cost is also referred to as budget at completion (BAC).
    *
    * @param val the amount to be set
    */
   public void setBaselineCost(Number val)
   {
      set(TaskField.BASELINE_COST, val);
   }

   /**
    * The Baseline Duration field shows the original span of time planned to
    * complete a task.
    *
    * @param val duration
    */
   public void setBaselineDuration(Duration val)
   {
      set(TaskField.BASELINE_DURATION, val);
   }

   /**
    * The Baseline Finish field shows the planned completion date for a
    * task at the time
    * you saved a baseline. Information in this field becomes available
    * when you set a
    * baseline for a task.
    *
    * @param val Date to be set
    */
   public void setBaselineFinish(Date val)
   {
      set(TaskField.BASELINE_FINISH, val);
   }

   /**
    * The Baseline Start field shows the planned beginning date for a task at
    * the time
    * you saved a baseline. Information in this field becomes available when you
    * set a baseline.
    *
    * @param val Date to be set
    */
   public void setBaselineStart(Date val)
   {
      set(TaskField.BASELINE_START, val);
   }

   /**
    * The Baseline Work field shows the originally planned amount of work to
    * be performed
    * by all resources assigned to a task. This field shows the planned
    * person-hours
    * scheduled for a task. Information in the Baseline Work field
    * becomes available
    * when you set a baseline for the project.
    *
    * @param val the duration to be set.
    */
   public void setBaselineWork(Duration val)
   {
      set(TaskField.BASELINE_WORK, val);
   }

   /**
    * The BCWP (budgeted cost of work performed) field contains the
    * cumulative value
    * of the assignment's timephased percent complete multiplied by
    * the assignments
    * timephased baseline cost. BCWP is calculated up to the status
    * date or todays
    * date. This information is also known as earned value.
    *
    * @param val the amount to be set
    */
   public void setBCWP(Number val)
   {
      set(TaskField.BCWP, val);
   }

   /**
    * The BCWS (budgeted cost of work scheduled) field contains the cumulative
    * timephased baseline costs up to the status date or today's date.
    *
    * @param val the amount to set
    */
   public void setBCWS(Number val)
   {
      set(TaskField.BCWS, val);
   }

   /**
    * The Confirmed field indicates whether all resources assigned to a task have
    * accepted or rejected the task assignment in response to a TeamAssign message
    * regarding their assignments.
    *
    * @param val boolean value
    */
   public void setConfirmed(boolean val)
   {
      set(TaskField.CONFIRMED, val);
   }

   /**
    * The Constraint Date field shows the specific date associated with certain
    * constraint types,
    *  such as Must Start On, Must Finish On, Start No Earlier Than,
    *  Start No Later Than,
    *  Finish No Earlier Than, and Finish No Later Than.
    *  SEE class constants
    *
    * @param val Date to be set
    */
   public void setConstraintDate(Date val)
   {
      set(TaskField.CONSTRAINT_DATE, val);
   }

   /**
    * Private method for dealing with string parameters from File.
    *
    * @param type string constraint type
    */
   public void setConstraintType(ConstraintType type)
   {
      set(TaskField.CONSTRAINT_TYPE, type);
   }

   /**
    * The Contact field contains the name of an individual
    * responsible for a task.
    *
    * @param val value to be set
    */
   public void setContact(String val)
   {
      set(TaskField.CONTACT, val);
   }

   /**
    * The Cost field shows the total scheduled, or projected, cost for a task,
    * based on costs already incurred for work performed by all resources assigned
    * to the task, in addition to the costs planned for the remaining work for the
    * assignment. This can also be referred to as estimate at completion (EAC).
    *
    * @param val amount
    */
   public void setCost(Number val)
   {
      set(TaskField.COST, val);
   }

   /**
    * Set a cost value.
    * 
    * @param index cost index (1-10)
    * @param value cost value
    */
   public void setCost(int index, Number value)
   {
      set(selectField(CUSTOM_COST, index), value);
   }

   /**
    * Retrieve a cost value.
    * 
    * @param index cost index (1-10)
    * @return cost value
    */
   public Number getCost(int index)
   {
      return (Number) getCachedValue(selectField(CUSTOM_COST, index));
   }

   /**
    * The Cost Variance field shows the difference between the
    * baseline cost and total cost for a task. The total cost is the
    * current estimate of costs based on actual costs and remaining costs.
    *
    * @param val amount
    */
   public void setCostVariance(Number val)
   {
      set(TaskField.COST_VARIANCE, val);
   }

   /**
    * The Created field contains the date and time when a task was
    * added to the project.
    *
    * @param val date
    */
   public void setCreateDate(Date val)
   {
      set(TaskField.CREATED, val);
   }

   /**
    * The Critical field indicates whether a task has any room in the
    * schedule to slip,
    * or if a task is on the critical path. The Critical field contains
    * Yes if the task
    * is critical and No if the task is not critical.
    *
    * @param val whether task is critical or not
    */
   public void setCritical(boolean val)
   {
      set(TaskField.CRITICAL, val);
   }

   /**
    * The CV (earned value cost variance) field shows the difference
    * between how much it should have cost to achieve the current level of
    * completion on the task, and how much it has actually cost to achieve the
    * current level of completion up to the status date or today's date.
    *
    * @param val value to set
    */
   public void setCV(Number val)
   {
      set(TaskField.CV, val);
   }

   /**
    * Set amount of delay as elapsed real time.
    *
    * @param val elapsed time
    */
   public void setLevelingDelay(Duration val)
   {
      set(TaskField.LEVELING_DELAY, val);
   }

   /**
    * The Duration field is the total span of active working time for a task.
    * This is generally the amount of time from the start to the finish of a task.
    * The default for new tasks is 1 day (1d).
    *
    * @param val duration
    */
   public void setDuration(Duration val)
   {
      set(TaskField.DURATION, val);
   }

   /**
    * Set the duration text used for a manually scheduled task.
    * 
    * @param val text
    */
   public void setDurationText(String val)
   {
      set(TaskField.DURATION_TEXT, val);
   }

   /**
    * Set the manual duration attribute.
    * 
    * @param dur manual duration
    */
   public void setManualDuration(Duration dur)
   {
      set(TaskField.MANUAL_DURATION, dur);
   }

   /**
    * Read the manual duration attribute.
    * 
    * @return manual duration
    */
   public Duration getManualDuration()
   {
      return (Duration) getCachedValue(TaskField.MANUAL_DURATION);
   }

   /**
    * The Duration Variance field contains the difference between the
    * baseline duration of a task and the forecast or actual duration
    * of the task.
    *
    * @param duration duration value
    */
   public void setDurationVariance(Duration duration)
   {
      set(TaskField.DURATION_VARIANCE, duration);
   }

   /**
    * The Early Finish field contains the earliest date that a task
    * could possibly finish, based on early finish dates of predecessor
    * and successor tasks, other constraints, and any leveling delay.
    *
    * @param date Date value
    */
   public void setEarlyFinish(Date date)
   {
      set(TaskField.EARLY_FINISH, date);
   }

   /**
    * The Early Start field contains the earliest date that a task could
    * possibly begin, based on the early start dates of predecessor and
    * successor tasks, and other constraints.
    *
    * @param date Date value
    */
   public void setEarlyStart(Date date)
   {
      set(TaskField.EARLY_START, date);
   }

   /**
    * The Finish field shows the date and time that a task is scheduled to be
    * completed. MS project allows a finish date to be entered, and will
    * calculate the duration, or a duration can be supplied and MS Project
    * will calculate the finish date.
    *
    * @param date Date value
    */
   public void setFinish(Date date)
   {
      set(TaskField.FINISH, date);
   }

   /**
    * Set the finish text used for a manually scheduled task.
    * 
    * @param val text
    */
   public void setFinishText(String val)
   {
      set(TaskField.FINISH_TEXT, val);
   }

   /**
    * The Finish Variance field contains the amount of time that represents the
    * difference between a task's baseline finish date and its forecast
    * or actual finish date.
    *
    * @param duration duration value
    */
   public void setFinishVariance(Duration duration)
   {
      set(TaskField.FINISH_VARIANCE, duration);
   }

   /**
    * The Fixed Cost field shows any task expense that is not associated
    * with a resource cost.
    *
    * @param val amount
    */
   public void setFixedCost(Number val)
   {
      set(TaskField.FIXED_COST, val);
   }

   /**
    * The Free Slack field contains the amount of time that a task can be
    * delayed without delaying any successor tasks. If the task has no
    * successors, free slack is the amount of time that a task can be delayed
    * without delaying the entire project's finish date.
    *
    * @param duration duration value
    */
   public void setFreeSlack(Duration duration)
   {
      set(TaskField.FREE_SLACK, duration);
   }

   /**
    * The Hide Bar flag indicates whether the Gantt bars and Calendar bars
    * for a task are hidden when this project's data is displayed in MS Project.
    *
    * @param flag boolean value
    */
   public void setHideBar(boolean flag)
   {
      set(TaskField.HIDEBAR, flag);
   }

   /**
    * The ID field contains the identifier number that Microsoft Project
    * automatically assigns to each task as you add it to the project.
    * The ID indicates the position of a task with respect to the other tasks.
    *
    * @param val ID
    */
   public void setID(Integer val)
   {
      ProjectFile parent = getParentFile();
      Integer previous = getID();

      if (previous != null)
      {
         parent.unmapTaskID(previous);
      }

      parent.mapTaskID(val, this);

      set(TaskField.ID, val);
   }

   /**
    * The Late Finish field contains the latest date that a task can finish
    * without delaying the finish of the project. This date is based on the
    * task's late start date, as well as the late start and late finish dates
    * of predecessor and successor tasks, and other constraints.
    *
    * @param date date value
    */
   public void setLateFinish(Date date)
   {
      set(TaskField.LATE_FINISH, date);
   }

   /**
    * The Late Start field contains the latest date that a task can start
    * without delaying the finish of the project. This date is based on the
    * task's start date, as well as the late start and late finish dates of
    * predecessor and successor tasks, and other constraints.
    *
    * @param date date value
    */
   public void setLateStart(Date date)
   {
      set(TaskField.LATE_START, date);
   }

   /**
    * The Linked Fields field indicates whether there are OLE links to the task,
    * either from elsewhere in the active project, another Microsoft Project
    * file, or from another program.
    *
    * @param flag boolean value
    */
   public void setLinkedFields(boolean flag)
   {
      set(TaskField.LINKED_FIELDS, flag);
   }

   /**
    * This is a user defined field used to mark a task for some form of
    * additional action.
    *
    * @param flag boolean value
    */
   public void setMarked(boolean flag)
   {
      set(TaskField.MARKED, flag);
   }

   /**
    * The Milestone field indicates whether a task is a milestone.
    *
    * @param flag boolean value
    */
   public void setMilestone(boolean flag)
   {
      set(TaskField.MILESTONE, flag);
   }

   /**
    * The Name field contains the name of a task.
    *
    * @param name task name
    */
   public void setName(String name)
   {
      set(TaskField.NAME, name);
   }

   /**
    * The Objects field contains the number of objects attached to a task.
    *
    * @param val - integer value
    */
   public void setObjects(Integer val)
   {
      set(TaskField.OBJECTS, val);
   }

   /**
    * The Outline Level field contains the number that indicates the level of
    * the task in the project outline hierarchy.
    *
    * @param val - int
    */
   public void setOutlineLevel(Integer val)
   {
      set(TaskField.OUTLINE_LEVEL, val);
   }

   /**
    * The Outline Number field contains the number of the task in the structure
    * of an outline. This number indicates the task's position within the
    * hierarchical structure of the project outline. The outline number is
    * similar to a WBS (work breakdown structure) number, except that the
    * outline number is automatically entered by Microsoft Project.
    *
    * @param val - text
    */
   public void setOutlineNumber(String val)
   {
      set(TaskField.OUTLINE_NUMBER, val);
   }

   /**
    * The Priority field provides choices for the level of importance
    * assigned to a task, which in turn indicates how readily a task can be
    * delayed or split during resource leveling.
    * The default priority is Medium. Those tasks with a priority
    * of Do Not Level are never delayed or split when Microsoft Project levels
    * tasks that have overallocated resources assigned.
    *
    * @param priority the priority value
    */
   public void setPriority(Priority priority)
   {
      set(TaskField.PRIORITY, priority);
   }

   /**
    * The Project field shows the name of the project from which a
    * task originated.
    * This can be the name of the active project file. If there are
    * other projects
    * inserted into the active project file, the name of the
    * inserted project appears
    * in this field for the task.
    *
    * @param val - text
    */
   public void setProject(String val)
   {
      set(TaskField.PROJECT, val);
   }

   /**
    * The Remaining Cost field shows the remaining scheduled expense of a task that
    * will be incurred in completing the remaining scheduled work by all resources
    * assigned to the task.
    *
    * @param val - currency amount
    */
   public void setRemainingCost(Number val)
   {
      set(TaskField.REMAINING_COST, val);
   }

   /**
    * The Remaining Duration field shows the amount of time required to complete
    * the unfinished portion of a task.
    *
    * @param val - duration.
    */
   public void setRemainingDuration(Duration val)
   {
      set(TaskField.REMAINING_DURATION, val);
   }

   /**
    * The Remaining Work field shows the amount of time, or person-hours,
    * still required by all assigned resources to complete a task.
    * @param val  - duration
    */
   public void setRemainingWork(Duration val)
   {
      set(TaskField.REMAINING_WORK, val);
   }

   /**
    * The Resource Group field contains the list of resource groups to which the
    * resources assigned to a task belong.
    *
    * @param val - String list
    */
   public void setResourceGroup(String val)
   {
      set(TaskField.RESOURCE_GROUP, val);
   }

   /**
    * The Resource Initials field lists the abbreviations for the names of
    * resources assigned to a task. These initials can serve as substitutes
    * for the names.
    *
    * Note that MS Project 98 does no normally populate this field when
    * it generates an MPX file, and will therefore not expect to see values
    * in this field when it reads an MPX file. Supplying values for this
    * field will cause MS Project 98, 2000, and 2002 to create new resources
    * and ignore any other resource assignments that have been defined
    * in the MPX file.
    *
    * @param val String containing a comma separated list of initials
    */
   public void setResourceInitials(String val)
   {
      set(TaskField.RESOURCE_INITIALS, val);
   }

   /**
    * The Resource Names field lists the names of all resources
    * assigned to a task.
    *
    * Note that MS Project 98 does not normally populate this field when
    * it generates an MPX file, and will therefore not expect to see values
    * in this field when it reads an MPX file. Supplying values for this
    * field when writing an MPX file will cause MS Project 98, 2000, and 2002 
    * to create new resources and ignore any other resource assignments 
    * that have been defined in the MPX file.
    *
    * @param val String containing a comma separated list of names
    */
   public void setResourceNames(String val)
   {
      set(TaskField.RESOURCE_NAMES, val);
   }

   /**
    * The Resume field shows the date that the remaining portion of a task is
    * scheduled to resume after you enter a new value for the % Complete field.
    * The Resume field is also recalculated when the remaining portion of a task
    * is moved to a new date.
    *
    * @param val - Date
    */
   public void setResume(Date val)
   {
      set(TaskField.RESUME, val);
   }

   /**
    * For subtasks, the Rollup field indicates whether information on the subtask
    * Gantt bars will be rolled up to the summary task bar. For summary tasks, the
    * Rollup field indicates whether the summary task bar displays rolled up bars.
    * You must have the Rollup field for summary tasks set to Yes for any subtasks
    * to roll up to them.
    *
    * @param val - boolean
    */
   public void setRollup(boolean val)
   {
      set(TaskField.ROLLUP, val);
   }

   /**
    * The Start field shows the date and time that a task is scheduled to begin.
    * You can enter the start date you want, to indicate the date when the task
    * should begin. Or, you can have Microsoft Project calculate the start date.
    * @param val - Date
    */
   public void setStart(Date val)
   {
      set(TaskField.START, val);
   }

   /**
    * Set the start text used for a manually scheduled task.
    * 
    * @param val text
    */
   public void setStartText(String val)
   {
      set(TaskField.START_TEXT, val);
   }

   /**
    * The Start Variance field contains the amount of time that represents the
    * difference between a task's baseline start date and its currently
    * scheduled start date.
    *
    * @param val - duration
    */
   public void setStartVariance(Duration val)
   {
      set(TaskField.START_VARIANCE, val);
   }

   /**
    * The Stop field shows the date that represents the end of the actual
    * portion of a task. Typically, Microsoft Project calculates the stop date.
    * However, you can edit this date as well.
    *
    * @param val - Date
    */
   public void setStop(Date val)
   {
      set(TaskField.STOP, val);
   }

   /**
    * The Subproject File field contains the name of a project inserted into
    * the active project file. The Subproject File field contains the inserted
    * project's path and file name.
    *
    * @param val - String
    */
   public void setSubprojectName(String val)
   {
      set(TaskField.SUBPROJECT_FILE, val);
   }

   /**
    * The Summary field indicates whether a task is a summary task.
    *
    * @param val - boolean
    */
   public void setSummary(boolean val)
   {
      set(TaskField.SUMMARY, val);
   }

   /**
    * The SV (earned value schedule variance) field shows the difference
    * in cost terms between the current progress and the baseline plan
    * of the task up to the status date or today's date. You can use SV
    * to check costs to determine whether tasks are on schedule.
    * @param val - currency amount
    */
   public void setSV(Number val)
   {
      set(TaskField.SV, val);
   }

   /**
    * The Total Slack field contains the amount of time a task can be delayed
    * without delaying the project's finish date.
    *
    * @param val - duration
    */
   public void setTotalSlack(Duration val)
   {
      set(TaskField.TOTAL_SLACK, val);
   }

   /**
    * The Unique ID field contains the number that Microsoft Project
    * automatically designates whenever a new task is created.
    * This number indicates the sequence in which the task was created,
    * regardless of placement in the schedule.
    *
    * @param val unique ID
    */
   public void setUniqueID(Integer val)
   {
      ProjectFile parent = getParentFile();
      Integer previous = getUniqueID();

      if (previous != null)
      {
         parent.unmapTaskUniqueID(previous);
      }

      parent.mapTaskUniqueID(val, this);

      set(TaskField.UNIQUE_ID, val);
   }

   /**
    * The Update Needed field indicates whether a TeamUpdate message should
    * be sent to the assigned resources because of changes to the start date,
    * finish date, or resource reassignments of the task.
    *
    * @param val - boolean
    */
   public void setUpdateNeeded(boolean val)
   {
      set(TaskField.UPDATE_NEEDED, val);
   }

   /**
    * The work breakdown structure code. The WBS field contains an alphanumeric
    * code you can use to represent the task's position within the hierarchical
    * structure of the project. This field is similar to the outline number,
    * except that you can edit it.
    *
    * @param val - String
    */
   public void setWBS(String val)
   {
      set(TaskField.WBS, val);
   }

   /**
    * The Work field shows the total amount of work scheduled to be performed
    * on a task by all assigned resources. This field shows the total work,
    * or person-hours, for a task.
    *
    * @param val - duration
    */
   public void setWork(Duration val)
   {
      set(TaskField.WORK, val);
   }

   /**
    * The Work Variance field contains the difference between a task's baseline
    * work and the currently scheduled work.
    *
    * @param val - duration
    */
   public void setWorkVariance(Duration val)
   {
      set(TaskField.WORK_VARIANCE, val);
   }

   /**
    * The % Complete field contains the current status of a task,
    * expressed as the percentage of the task's duration that has been completed.
    * You can enter percent complete, or you can have Microsoft Project calculate
    * it for you based on actual duration.
    * @return percentage as float
    */
   public Number getPercentageComplete()
   {
      return ((Number) getCachedValue(TaskField.PERCENT_COMPLETE));
   }

   /**
    * The % Work Complete field contains the current status of a task,
    * expressed as the percentage of the task's work that has been completed.
    * You can enter percent work complete, or you can have Microsoft Project
    * calculate it for you based on actual work on the task.
    *
    * @return percentage as float
    */
   public Number getPercentageWorkComplete()
   {
      return ((Number) getCachedValue(TaskField.PERCENT_WORK_COMPLETE));
   }

   /**
    * The Actual Cost field shows costs incurred for work already performed
    * by all resources on a task, along with any other recorded costs associated
    * with the task. You can enter all the actual costs or have Microsoft Project
    * calculate them for you.
    *
    * @return currency amount as float
    */
   public Number getActualCost()
   {
      return ((Number) getCachedValue(TaskField.ACTUAL_COST));
   }

   /**
    * The Actual Duration field shows the span of actual working time for a
    * task so far, based on the scheduled duration and current remaining work
    * or completion percentage.
    *
    * @return duration string
    */
   public Duration getActualDuration()
   {
      return ((Duration) getCachedValue(TaskField.ACTUAL_DURATION));
   }

   /**
    * The Actual Finish field shows the date and time that a task actually
    * finished. Microsoft Project sets the Actual Finish field to the scheduled
    * finish date if the completion percentage is 100. This field contains "NA"
    * until you enter actual information or set the completion percentage to 100.
    * If "NA" is entered as value, arbitrary year zero Date is used. Date(0);
    *
    * @return Date
    */
   public Date getActualFinish()
   {
      return ((Date) getCachedValue(TaskField.ACTUAL_FINISH));
   }

   /**
    * The Actual Start field shows the date and time that a task actually began.
    * When a task is first created, the Actual Start field contains "NA." Once
    * you enter the first actual work or a completion percentage for a task,
    * Microsoft Project sets the actual start date to the scheduled start date.
    * If "NA" is entered as value, arbitrary year zero Date is used. Date(0);
    *
    * @return Date
    */
   public Date getActualStart()
   {
      return ((Date) getCachedValue(TaskField.ACTUAL_START));
   }

   /**
    * The Actual Work field shows the amount of work that has already been done
    * by the resources assigned to a task.
    *
    * @return duration string
    */
   public Duration getActualWork()
   {
      return ((Duration) getCachedValue(TaskField.ACTUAL_WORK));
   }

   /**
    * The Baseline Cost field shows the total planned cost for a task.
    * Baseline cost is also referred to as budget at completion (BAC).
    * @return currency amount as float
    */
   public Number getBaselineCost()
   {
      return ((Number) getCachedValue(TaskField.BASELINE_COST));
   }

   /**
    * The Baseline Duration field shows the original span of time planned
    * to complete a task.
    *
    * @return  - duration string
    */
   public Duration getBaselineDuration()
   {
      Object result = getCachedValue(TaskField.BASELINE_DURATION);
      if (result == null)
      {
         result = getCachedValue(TaskField.BASELINE_ESTIMATED_DURATION);
      }

      if (!(result instanceof Duration))
      {
         result = null;
      }
      return (Duration) result;
   }

   /**
    * Retrieves the text value for the baseline duration.
    * 
    * @return baseline duration text
    */
   public String getBaselineDurationText()
   {
      Object result = getCachedValue(TaskField.BASELINE_DURATION);
      if (result == null)
      {
         result = getCachedValue(TaskField.BASELINE_ESTIMATED_DURATION);
      }

      if (!(result instanceof String))
      {
         result = null;
      }
      return (String) result;
   }

   /**
    * Sets the baseline duration text value.
    * 
    * @param value baseline duration text
    */
   public void setBaselineDurationText(String value)
   {
      set(TaskField.BASELINE_DURATION, value);
   }

   /**
    * The Baseline Finish field shows the planned completion date for a task
    * at the time you saved a baseline. Information in this field becomes
    * available when you set a baseline for a task.
    *
    * @return Date
    */
   public Date getBaselineFinish()
   {
      Object result = getCachedValue(TaskField.BASELINE_FINISH);
      if (result == null)
      {
         result = getCachedValue(TaskField.BASELINE_ESTIMATED_FINISH);
      }

      if (!(result instanceof Date))
      {
         result = null;
      }
      return (Date) result;
   }

   /**
    * Retrieves the baseline finish text value.
    * 
    * @return baseline finish text
    */
   public String getBaselineFinishText()
   {
      Object result = getCachedValue(TaskField.BASELINE_FINISH);
      if (result == null)
      {
         result = getCachedValue(TaskField.BASELINE_ESTIMATED_FINISH);
      }

      if (!(result instanceof String))
      {
         result = null;
      }
      return (String) result;
   }

   /**
    * Sets the baseline finish text value.
    * 
    * @param value baseline finish text
    */
   public void setBaselineFinishText(String value)
   {
      set(TaskField.BASELINE_FINISH, value);
   }

   /**
    * The Baseline Start field shows the planned beginning date for a task at
    * the time you saved a baseline. Information in this field becomes available
    * when you set a baseline.
    *
    * @return Date
    */
   public Date getBaselineStart()
   {
      Object result = getCachedValue(TaskField.BASELINE_START);
      if (result == null)
      {
         result = getCachedValue(TaskField.BASELINE_ESTIMATED_START);
      }

      if (!(result instanceof Date))
      {
         result = null;
      }
      return (Date) result;
   }

   /**
    * Retrieves the baseline start text value.
    * 
    * @return baseline start value
    */
   public String getBaselineStartText()
   {
      Object result = getCachedValue(TaskField.BASELINE_START);
      if (result == null)
      {
         result = getCachedValue(TaskField.BASELINE_ESTIMATED_START);
      }

      if (!(result instanceof String))
      {
         result = null;
      }
      return (String) result;
   }

   /**
    * Sets the baseline start text value.
    * 
    * @param value baseline start text
    */
   public void setBaselineStartText(String value)
   {
      set(TaskField.BASELINE_START, value);
   }

   /**
    * The Baseline Work field shows the originally planned amount of work to be
    * performed by all resources assigned to a task. This field shows the planned
    * person-hours scheduled for a task. Information in the Baseline Work field
    * becomes available when you set a baseline for the project.
    *
    * @return Duration
    */
   public Duration getBaselineWork()
   {
      return ((Duration) getCachedValue(TaskField.BASELINE_WORK));
   }

   /**
    * The BCWP (budgeted cost of work performed) field contains
    * the cumulative value of the assignment's timephased percent complete
    * multiplied by the assignment's timephased baseline cost.
    * BCWP is calculated up to the status date or today's date.
    * This information is also known as earned value.
    *
    * @return currency amount as float
    */
   public Number getBCWP()
   {
      return ((Number) getCachedValue(TaskField.BCWP));
   }

   /**
    * The BCWS (budgeted cost of work scheduled) field contains the cumulative
    * timephased baseline costs up to the status date or today's date.
    *
    * @return currency amount as float
    */
   public Number getBCWS()
   {
      return ((Number) getCachedValue(TaskField.BCWS));
   }

   /**
    * The Confirmed field indicates whether all resources assigned to a task
    * have accepted or rejected the task assignment in response to a TeamAssign
    * message regarding their assignments.
    *
    * @return boolean
    */
   public boolean getConfirmed()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.CONFIRMED)));
   }

   /**
    * The Constraint Date field shows the specific date associated with certain
    * constraint types, such as Must Start On, Must Finish On,
    * Start No Earlier Than,
    * Start No Later Than, Finish No Earlier Than, and Finish No Later Than.
    *
    * @return Date
    */
   public Date getConstraintDate()
   {
      return ((Date) getCachedValue(TaskField.CONSTRAINT_DATE));
   }

   /**
    * The Constraint Type field provides choices for the type of constraint you
    * can apply for scheduling a task.
    *
    * @return constraint type
    */
   public ConstraintType getConstraintType()
   {
      return ((ConstraintType) getCachedValue(TaskField.CONSTRAINT_TYPE));
   }

   /**
    * The Contact field contains the name of an individual
    * responsible for a task.
    *
    * @return String
    */
   public String getContact()
   {
      return ((String) getCachedValue(TaskField.CONTACT));
   }

   /**
    * The Cost field shows the total scheduled, or projected, cost for a task,
    * based on costs already incurred for work performed by all resources assigned
    * to the task, in addition to the costs planned for the remaining work for the
    * assignment. This can also be referred to as estimate at completion (EAC).
    *
    * @return cost amount
    */
   public Number getCost()
   {
      return ((Number) getCachedValue(TaskField.COST));
   }

   /**
    * The Cost Variance field shows the difference between the baseline cost
    * and total cost for a task. The total cost is the current estimate of costs
    * based on actual costs and remaining costs.
    *
    * @return amount
    */
   public Number getCostVariance()
   {
      Number variance = (Number) getCachedValue(TaskField.COST_VARIANCE);
      if (variance == null)
      {
         Number cost = getCost();
         Number baselineCost = getBaselineCost();
         if (cost != null && baselineCost != null)
         {
            variance = NumberUtility.getDouble(cost.doubleValue() - baselineCost.doubleValue());
            set(TaskField.COST_VARIANCE, variance);
         }
      }
      return (variance);
   }

   /**
    * The Created field contains the date and time when a task was added
    * to the project.
    *
    * @return Date
    */
   public Date getCreateDate()
   {
      return ((Date) getCachedValue(TaskField.CREATED));
   }

   /**
    * The Critical field indicates whether a task has any room in the schedule
    * to slip, or if a task is on the critical path. The Critical field contains
    * Yes if the task is critical and No if the task is not critical.
    *
    * @return boolean
    */
   public boolean getCritical()
   {
      Boolean critical = (Boolean) getCachedValue(TaskField.CRITICAL);
      if (critical == null)
      {
         Duration totalSlack = getTotalSlack();
         critical = Boolean.valueOf(totalSlack != null && totalSlack.getDuration() <= 0 && NumberUtility.getInt(getPercentageComplete()) != 100 && ((getTaskMode() == TaskMode.AUTO_SCHEDULED) || (getDurationText() == null && getStartText() == null && getFinishText() == null)));
         set(TaskField.CRITICAL, critical);
      }
      return (BooleanUtility.getBoolean(critical));
   }

   /**
    * The CV (earned value cost variance) field shows the difference between
    * how much it should have cost to achieve the current level of completion
    * on the task, and how much it has actually cost to achieve the current
    * level of completion up to the status date or today's date.
    * How Calculated   CV is the difference between BCWP
    * (budgeted cost of work performed) and ACWP
    * (actual cost of work performed). Microsoft Project calculates
    * the CV as follows: CV = BCWP - ACWP
    *
    * @return sum of earned value cost variance
    */
   public Number getCV()
   {
      Number variance = (Number) getCachedValue(TaskField.CV);
      if (variance == null)
      {
         variance = Double.valueOf(NumberUtility.getDouble(getBCWP()) - NumberUtility.getDouble(getACWP()));
         set(TaskField.CV, variance);
      }
      return (variance);
   }

   /**
    * Delay , in MPX files as eg '0ed'. Use duration
    *
    * @return Duration
    */
   public Duration getLevelingDelay()
   {
      return ((Duration) getCachedValue(TaskField.LEVELING_DELAY));
   }

   /**
    * The Duration field is the total span of active working time for a task.
    * This is generally the amount of time from the start to the finish of a task.
    * The default for new tasks is 1 day (1d).
    *
    * @return Duration
    */
   public Duration getDuration()
   {
      return (Duration) getCachedValue(TaskField.DURATION);
   }

   /**
    * Retrieves the duration text of a manually scheduled task.
    * 
    * @return duration text
    */
   public String getDurationText()
   {
      return (String) getCachedValue(TaskField.DURATION_TEXT);
   }

   /**
    * Set a duration value.
    * 
    * @param index duration index (1-10)
    * @param value duration value
    */
   public void setDuration(int index, Duration value)
   {
      set(selectField(CUSTOM_DURATION, index), value);
   }

   /**
    * Retrieve a duration value.
    * 
    * @param index duration index (1-10)
    * @return duration value
    */
   public Duration getDuration(int index)
   {
      return (Duration) getCachedValue(selectField(CUSTOM_DURATION, index));
   }

   /**
    * The Duration Variance field contains the difference between the
    * baseline duration of a task and the total duration (current estimate)
    * of a task.
    *
    * @return Duration
    */
   public Duration getDurationVariance()
   {
      Duration variance = (Duration) getCachedValue(TaskField.DURATION_VARIANCE);
      if (variance == null)
      {
         Duration duration = getDuration();
         Duration baselineDuration = getBaselineDuration();

         if (duration != null && baselineDuration != null)
         {
            variance = Duration.getInstance(duration.getDuration() - baselineDuration.convertUnits(duration.getUnits(), getParentFile().getProjectHeader()).getDuration(), duration.getUnits());
            set(TaskField.DURATION_VARIANCE, variance);
         }
      }
      return (variance);
   }

   /**
    * The Early Finish field contains the earliest date that a task could
    * possibly finish, based on early finish dates of predecessor and
    * successor tasks, other constraints, and any leveling delay.
    *
    * @return Date
    */
   public Date getEarlyFinish()
   {
      return ((Date) getCachedValue(TaskField.EARLY_FINISH));
   }

   /**
    * The Early Start field contains the earliest date that a task could
    * possibly begin, based on the early start dates of predecessor and
    * successor tasks, and other constraints.
    *
    * @return Date
    */
   public Date getEarlyStart()
   {
      return ((Date) getCachedValue(TaskField.EARLY_START));
   }

   /**
    * The Finish field shows the date and time that a task is scheduled to
    * be completed. You can enter the finish date you want, to indicate the
    * date when the task should be completed. Or, you can have Microsoft
    * Project calculate the finish date.
    *
    * @return Date
    */
   public Date getFinish()
   {
      return (Date) getCachedValue(TaskField.FINISH);
   }

   /**
    * Retrieves the finish text of a manually scheduled task.
    * 
    * @return finish text
    */
   public String getFinishText()
   {
      return (String) getCachedValue(TaskField.FINISH_TEXT);
   }

   /**
    * Set a finish value.
    * 
    * @param index finish index (1-10)
    * @param value finish value
    */
   public void setFinish(int index, Date value)
   {
      set(selectField(CUSTOM_FINISH, index), value);
   }

   /**
    * Retrieve a finish value.
    * 
    * @param index finish index (1-10)
    * @return finish value
    */
   public Date getFinish(int index)
   {
      return (Date) getCachedValue(selectField(CUSTOM_FINISH, index));
   }

   /**
    * Calculate the finish variance.
    *
    * @return finish variance
    */
   public Duration getFinishVariance()
   {
      Duration variance = (Duration) getCachedValue(TaskField.FINISH_VARIANCE);
      if (variance == null)
      {
         TimeUnit format = getParentFile().getProjectHeader().getDefaultDurationUnits();
         variance = DateUtility.getVariance(this, getBaselineFinish(), getFinish(), format);
         set(TaskField.FINISH_VARIANCE, variance);
      }
      return (variance);
   }

   /**
    * The Fixed Cost field shows any task expense that is not associated
    * with a resource cost.
    *
    * @return currency amount
    */
   public Number getFixedCost()
   {
      return ((Number) getCachedValue(TaskField.FIXED_COST));
   }

   /**
    * Set a flag value.
    * 
    * @param index flag index (1-20)
    * @param value flag value
    */
   public void setFlag(int index, boolean value)
   {
      set(selectField(CUSTOM_FLAG, index), value);
   }

   /**
    * Retrieve a flag value.
    * 
    * @param index flag index (1-20)
    * @return flag value
    */
   public boolean getFlag(int index)
   {
      return BooleanUtility.getBoolean((Boolean) getCachedValue(selectField(CUSTOM_FLAG, index)));
   }

   /**
    * The Free Slack field contains the amount of time that a task can be
    * delayed without delaying any successor tasks. If the task has no
    * successors, free slack is the amount of time that a task can be
    * delayed without delaying the entire project's finish date.
    *
    * @return Duration
    */
   public Duration getFreeSlack()
   {
      return ((Duration) getCachedValue(TaskField.FREE_SLACK));
   }

   /**
    * The Hide Bar field indicates whether the Gantt bars and Calendar bars
    * for a task are hidden. Click Yes in the Hide Bar field to hide the
    * bar for the task. Click No in the Hide Bar field to show the bar
    * for the task.
    *
    * @return boolean
    */
   public boolean getHideBar()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.HIDEBAR)));
   }

   /**
    * The ID field contains the identifier number that Microsoft Project
    * automatically assigns to each task as you add it to the project.
    * The ID indicates the position of a task with respect to the other tasks.
    *
    * @return the task ID
    */
   public Integer getID()
   {
      return ((Integer) getCachedValue(TaskField.ID));
   }

   /**
    * The Late Finish field contains the latest date that a task can finish
    * without delaying the finish of the project. This date is based on the
    * task's late start date, as well as the late start and late finish
    * dates of predecessor and successor
    * tasks, and other constraints.
    *
    * @return Date
    */
   public Date getLateFinish()
   {
      return ((Date) getCachedValue(TaskField.LATE_FINISH));
   }

   /**
    * The Late Start field contains the latest date that a task can start
    * without delaying the finish of the project. This date is based on
    * the task's start date, as well as the late start and late finish
    * dates of predecessor and successor tasks, and other constraints.
    *
    * @return Date
    */
   public Date getLateStart()
   {
      return ((Date) getCachedValue(TaskField.LATE_START));
   }

   /**
    * The Linked Fields field indicates whether there are OLE links to the task,
    * either from elsewhere in the active project, another Microsoft Project file,
    * or from another program.
    *
    * @return boolean
    */
   public boolean getLinkedFields()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.LINKED_FIELDS)));
   }

   /**
    * The Marked field indicates whether a task is marked for further action or
    * identification of some kind. To mark a task, click Yes in the Marked field.
    * If you don't want a task marked, click No.
    *
    * @return true for marked
    */
   public boolean getMarked()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.MARKED)));
   }

   /**
    * The Milestone field indicates whether a task is a milestone.
    *
    * @return boolean
    */
   public boolean getMilestone()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.MILESTONE)));
   }

   /**
    * Retrieves the task name.
    *
    * @return task name
    */
   public String getName()
   {
      return ((String) getCachedValue(TaskField.NAME));
   }

   /**
    * The Notes field contains notes that you can enter about a task.
    * You can use task notes to help maintain a history for a task.
    *
    * @return notes
    */
   public String getNotes()
   {
      String notes = (String) getCachedValue(TaskField.NOTES);
      return (notes == null ? "" : notes);
   }

   /**
    * Set a number value.
    * 
    * @param index number index (1-20)
    * @param value number value
    */
   public void setNumber(int index, Number value)
   {
      set(selectField(CUSTOM_NUMBER, index), value);
   }

   /**
    * Retrieve a number value.
    * 
    * @param index number index (1-20)
    * @return number value
    */
   public Number getNumber(int index)
   {
      return (Number) getCachedValue(selectField(CUSTOM_NUMBER, index));
   }

   /**
    * The Objects field contains the number of objects attached to a task.
    * Microsoft Project counts the number of objects linked or embedded to a task.
    * However, objects in the Notes box in the Resource Form are not included
    * in this count.
    *
    * @return int
    */
   public Integer getObjects()
   {
      return ((Integer) getCachedValue(TaskField.OBJECTS));
   }

   /**
    * The Outline Level field contains the number that indicates the level
    * of the task in the project outline hierarchy.
    *
    * @return int
    */
   public Integer getOutlineLevel()
   {
      return ((Integer) getCachedValue(TaskField.OUTLINE_LEVEL));
   }

   /**
    * The Outline Number field contains the number of the task in the structure
    * of an outline. This number indicates the task's position within the
    * hierarchical structure of the project outline. The outline number is
    * similar to a WBS (work breakdown structure) number,
    * except that the outline number is automatically entered by
    * Microsoft Project.
    *
    * @return String
    */
   public String getOutlineNumber()
   {
      return ((String) getCachedValue(TaskField.OUTLINE_NUMBER));
   }

   /**
    * Retrieves the list of predecessors for this task.
    * 
    * @return list of predecessor Relation instances
    */
   @SuppressWarnings("unchecked") public List<Relation> getPredecessors()
   {
      return ((List<Relation>) getCachedValue(TaskField.PREDECESSORS));
   }

   /**
    * Retrieves the list of succesors for this task.
    * 
    * @return list of successor Relation instances
    */
   @SuppressWarnings("unchecked") public List<Relation> getSuccessors()
   {
      return ((List<Relation>) getCachedValue(TaskField.SUCCESSORS));
   }

   /**
    * The Priority field provides choices for the level of importance
    * assigned to a task, which in turn indicates how readily a task can be
    * delayed or split during resource leveling.
    * The default priority is Medium. Those tasks with a priority
    * of Do Not Level are never delayed or split when Microsoft Project levels
    * tasks that have overallocated resources assigned.
    *
    * @return priority class instance
    */
   public Priority getPriority()
   {
      return ((Priority) getCachedValue(TaskField.PRIORITY));
   }

   /**
    * The Project field shows the name of the project from which a task
    * originated.
    * This can be the name of the active project file. If there are other
    * projects inserted
    * into the active project file, the name of the inserted project appears
    * in this field
    * for the task.
    *
    * @return name of originating project
    */
   public String getProject()
   {
      return ((String) getCachedValue(TaskField.PROJECT));
   }

   /**
    * The Remaining Cost field shows the remaining scheduled expense of a
    * task that will be incurred in completing the remaining scheduled work
    * by all resources assigned to the task.
    *
    * @return remaining cost
    */
   public Number getRemainingCost()
   {
      return ((Number) getCachedValue(TaskField.REMAINING_COST));
   }

   /**
    * The Remaining Duration field shows the amount of time required
    * to complete the unfinished portion of a task.
    *
    * @return Duration
    */
   public Duration getRemainingDuration()
   {
      return ((Duration) getCachedValue(TaskField.REMAINING_DURATION));
   }

   /**
    * The Remaining Work field shows the amount of time, or person-hours,
    * still required by all assigned resources to complete a task.
    *
    * @return the amount of time still required to complete a task
    */
   public Duration getRemainingWork()
   {
      return ((Duration) getCachedValue(TaskField.REMAINING_WORK));
   }

   /**
    * The Resource Group field contains the list of resource groups to which
    * the resources assigned to a task belong.
    *
    * @return single string list of groups
    */
   public String getResourceGroup()
   {
      return ((String) getCachedValue(TaskField.RESOURCE_GROUP));
   }

   /**
    * The Resource Initials field lists the abbreviations for the names of
    * resources assigned to a task. These initials can serve as substitutes
    * for the names.
    *
    * Note that MS Project 98 does not export values for this field when
    * writing an MPX file, and the field is not currently populated by MPXJ
    * when reading an MPP file.
    *
    * @return String containing a comma separated list of initials
    */
   public String getResourceInitials()
   {
      return ((String) getCachedValue(TaskField.RESOURCE_INITIALS));
   }

   /**
    * The Resource Names field lists the names of all resources assigned
    * to a task.
    *
    * Note that MS Project 98 does not export values for this field when
    * writing an MPX file, and the field is not currently populated by MPXJ
    * when reading an MPP file.
    *
    * @return String containing a comma separated list of names
    */
   public String getResourceNames()
   {
      return ((String) getCachedValue(TaskField.RESOURCE_NAMES));
   }

   /**
    * The Resume field shows the date that the remaining portion of a task
    * is scheduled to resume after you enter a new value for the % Complete
    * field. The Resume field is also recalculated when the remaining portion
    * of a task is moved to a new date.
    *
    * @return Date
    */
   public Date getResume()
   {
      return ((Date) getCachedValue(TaskField.RESUME));
   }

   /**
    * For subtasks, the Rollup field indicates whether information on the
    * subtask Gantt bars
    * will be rolled up to the summary task bar. For summary tasks, the
    * Rollup field indicates
    * whether the summary task bar displays rolled up bars. You must
    * have the Rollup field for
    * summary tasks set to Yes for any subtasks to roll up to them.
    *
    * @return boolean
    */
   public boolean getRollup()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.ROLLUP)));
   }

   /**
    * The Start field shows the date and time that a task is scheduled to begin.
    * You can enter the start date you want, to indicate the date when the task
    * should begin. Or, you can have Microsoft Project calculate the start date.
    *
    * @return Date
    */
   public Date getStart()
   {
      return (Date) getCachedValue(TaskField.START);
   }

   /**
    * Retrieve the start text for a manually scheduled task.
    * 
    * @return start text
    */
   public String getStartText()
   {
      return (String) getCachedValue(TaskField.START_TEXT);
   }

   /**
    * Set a start value.
    * 
    * @param index start index (1-10)
    * @param value start value
    */
   public void setStart(int index, Date value)
   {
      set(selectField(CUSTOM_START, index), value);
   }

   /**
    * Retrieve a start value.
    * 
    * @param index start index (1-10)
    * @return start value
    */
   public Date getStart(int index)
   {
      return (Date) getCachedValue(selectField(CUSTOM_START, index));
   }

   /**
    * Calculate the start variance.
    * 
    * @return start variance
    */
   public Duration getStartVariance()
   {
      Duration variance = (Duration) getCachedValue(TaskField.START_VARIANCE);
      if (variance == null)
      {
         TimeUnit format = getParentFile().getProjectHeader().getDefaultDurationUnits();
         variance = DateUtility.getVariance(this, getBaselineStart(), getStart(), format);
         set(TaskField.START_VARIANCE, variance);
      }
      return (variance);
   }

   /**
    * The Stop field shows the date that represents the end of the actual
    * portion of a task. Typically, Microsoft Project calculates the stop date.
    * However, you can edit this date as well.
    *
    * @return Date
    */
   public Date getStop()
   {
      return ((Date) getCachedValue(TaskField.STOP));
   }

   /**
    * Contains the file name and path of the sub project represented by
    * the current task.
    *
    * @return sub project file path
    */
   public String getSubprojectName()
   {
      return ((String) getCachedValue(TaskField.SUBPROJECT_FILE));
   }

   /**
    * The Summary field indicates whether a task is a summary task.
    *
    * @return boolean, true-is summary task
    */
   public boolean getSummary()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.SUMMARY)));
   }

   /**
    * The SV (earned value schedule variance) field shows the difference in
    * cost terms between the current progress and the baseline plan of the
    * task up to the status date or today's date. You can use SV to
    * check costs to determine whether tasks are on schedule.
    *
    * @return -earned value schedule variance
    */
   public Number getSV()
   {
      Number variance = (Number) getCachedValue(TaskField.SV);
      if (variance == null)
      {
         Number bcwp = getBCWP();
         Number bcws = getBCWS();
         if (bcwp != null && bcws != null)
         {
            variance = NumberUtility.getDouble(bcwp.doubleValue() - bcws.doubleValue());
            set(TaskField.SV, variance);
         }
      }
      return (variance);
   }

   /**
    * Set a text value.
    * 
    * @param index text index (1-30)
    * @param value text value
    */
   public void setText(int index, String value)
   {
      set(selectField(CUSTOM_TEXT, index), value);
   }

   /**
    * Retrieve a text value.
    * 
    * @param index text index (1-30)
    * @return text value
    */
   public String getText(int index)
   {
      return (String) getCachedValue(selectField(CUSTOM_TEXT, index));
   }

   /**
    * Set an outline code value.
    * 
    * @param index outline code index (1-10)
    * @param value outline code value
    */
   public void setOutlineCode(int index, String value)
   {
      set(selectField(CUSTOM_OUTLINE_CODE, index), value);
   }

   /**
    * Retrieve an outline code value.
    * 
    * @param index outline code index (1-10)
    * @return outline code value
    */
   public String getOutlineCode(int index)
   {
      return (String) getCachedValue(selectField(CUSTOM_OUTLINE_CODE, index));
   }

   /**
    * The Total Slack field contains the amount of time a task can be
    * delayed without delaying the project's finish date.
    *
    * @return string representing duration
    */
   public Duration getTotalSlack()
   {
      Duration totalSlack = (Duration) getCachedValue(TaskField.TOTAL_SLACK);
      if (totalSlack == null)
      {
         Duration duration = getDuration();
         if (duration == null)
         {
            duration = Duration.getInstance(0, TimeUnit.DAYS);
         }

         TimeUnit units = duration.getUnits();

         Duration startSlack = getStartSlack();
         if (startSlack == null)
         {
            startSlack = Duration.getInstance(0, units);
         }
         else
         {
            if (startSlack.getUnits() != units)
            {
               startSlack = startSlack.convertUnits(units, getParentFile().getProjectHeader());
            }
         }

         Duration finishSlack = getFinishSlack();
         if (finishSlack == null)
         {
            finishSlack = Duration.getInstance(0, units);
         }
         else
         {
            if (finishSlack.getUnits() != units)
            {
               finishSlack = finishSlack.convertUnits(units, getParentFile().getProjectHeader());
            }
         }

         double startSlackDuration = startSlack.getDuration();
         double finishSlackDuration = finishSlack.getDuration();

         if (startSlackDuration == 0 || finishSlackDuration == 0)
         {
            if (startSlackDuration != 0)
            {
               totalSlack = startSlack;
            }
            else
            {
               totalSlack = finishSlack;
            }
         }
         else
         {
            if (startSlackDuration < finishSlackDuration)
            {
               totalSlack = startSlack;
            }
            else
            {
               totalSlack = finishSlack;
            }
         }

         set(TaskField.TOTAL_SLACK, totalSlack);
      }

      return (totalSlack);
   }

   /**
    * The Unique ID field contains the number that Microsoft Project
    * automatically designates whenever a new task is created. This number
    * indicates the sequence in which the task was
    * created, regardless of placement in the schedule.
    *
    * @return String
    */
   public Integer getUniqueID()
   {
      return ((Integer) getCachedValue(TaskField.UNIQUE_ID));
   }

   /**
    * The Update Needed field indicates whether a TeamUpdate message
    * should be sent to the assigned resources because of changes to the
    * start date, finish date, or resource reassignments of the task.
    *
    * @return true if needed.
    */
   public boolean getUpdateNeeded()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.UPDATE_NEEDED)));
   }

   /**
    * The work breakdown structure code. The WBS field contains an
    * alphanumeric code you can use to represent the task's position within
    * the hierarchical structure of the project. This field is similar to
    * the outline number, except that you can edit it.
    *
    * @return string
    */
   public String getWBS()
   {
      return ((String) getCachedValue(TaskField.WBS));
   }

   /**
    * The Work field shows the total amount of work scheduled to be performed
    * on a task by all assigned resources. This field shows the total work,
    * or person-hours, for a task.
    *
    * @return Duration representing duration .
    */
   public Duration getWork()
   {
      return ((Duration) getCachedValue(TaskField.WORK));
   }

   /**
    * The Work Variance field contains the difference between a task's
    * baseline work and the currently scheduled work.
    *
    * @return Duration representing duration.
    */
   public Duration getWorkVariance()
   {
      Duration variance = (Duration) getCachedValue(TaskField.WORK_VARIANCE);
      if (variance == null)
      {
         Duration work = getWork();
         Duration baselineWork = getBaselineWork();
         if (work != null && baselineWork != null)
         {
            variance = Duration.getInstance(work.getDuration() - baselineWork.convertUnits(work.getUnits(), getParentFile().getProjectHeader()).getDuration(), work.getUnits());
            set(TaskField.WORK_VARIANCE, variance);
         }
      }
      return (variance);
   }

   /**
    * Retrieve count of the number of child tasks.
    *
    * @return Number of child tasks.
    */
   int getChildTaskCount()
   {
      return (m_children.size());
   }

   /**
    * This method retrieves a reference to the parent of this task, as
    * defined by the outline level. If this task is at the top level,
    * this method will return null.
    *
    * @return parent task
    */
   public Task getParentTask()
   {
      return (m_parent);
   }

   /**
    * This method retrieves a list of child tasks relative to the
    * current task, as defined by the outine level. If there
    * are no child tasks, this method will return an empty list.
    *
    * @return child tasks
    */
   public List<Task> getChildTasks()
   {
      return (m_children);
   }

   /**
    * This method implements the only method in the Comparable interface.
    * This allows Tasks to be compared and sorted based on their ID value.
    * Note that if the MPX/MPP file has been generated by MSP, the ID value
    * will always be in the correct sequence. The Unique ID value will not
    * necessarily be in the correct sequence as task insertions and deletions
    * will change the order.
    *
    * @param o object to compare this instance with
    * @return result of comparison
    */
   @Override public int compareTo(Task o)
   {
      int id1 = NumberUtility.getInt(getID());
      int id2 = NumberUtility.getInt(o.getID());
      return ((id1 < id2) ? (-1) : ((id1 == id2) ? 0 : 1));
   }

   /**
    * This method retrieves a flag indicating whether the duration of the
    * task has only been estimated.
    *
    * @return boolean
    */
   public boolean getEstimated()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.ESTIMATED)));
   }

   /**
    * This method retrieves a flag indicating whether the duration of the
    * task has only been estimated.

    * @param estimated Boolean flag
    */
   public void setEstimated(boolean estimated)
   {
      set(TaskField.ESTIMATED, estimated);
   }

   /**
    * This method retrieves the deadline for this task.
    *
    * @return Task deadline
    */
   public Date getDeadline()
   {
      return ((Date) getCachedValue(TaskField.DEADLINE));
   }

   /**
    * This method sets the deadline for this task.
    *
    * @param deadline deadline date
    */
   public void setDeadline(Date deadline)
   {
      set(TaskField.DEADLINE, deadline);
   }

   /**
    * This method retrieves the task type.
    *
    * @return int representing the task type
    */
   public TaskType getType()
   {
      return ((TaskType) getCachedValue(TaskField.TYPE));
   }

   /**
    * This method sets the task type.
    *
    * @param type task type
    */
   public void setType(TaskType type)
   {
      set(TaskField.TYPE, type);
   }

   /**
    * Retrieves the flag indicating if this is a null task.
    *
    * @return boolean flag
    */
   public boolean getNull()
   {
      return (m_null);
   }

   /**
    * Sets the flag indicating if this is a null task.
    *
    * @param isNull boolean flag
    */
   public void setNull(boolean isNull)
   {
      m_null = isNull;
   }

   /**
    * Retrieve the WBS level.
    *
    * @return WBS level
    */
   public String getWBSLevel()
   {
      return (m_wbsLevel);
   }

   /**
    * Set the WBS level.
    *
    * @param wbsLevel WBS level
    */
   public void setWBSLevel(String wbsLevel)
   {
      m_wbsLevel = wbsLevel;
   }

   /**
    * Retrieve the resume valid flag.
    *
    * @return resume valie flag
    */
   public boolean getResumeValid()
   {
      return (m_resumeValid);
   }

   /**
    * Set the resume valid flag.
    *
    * @param resumeValid resume valid flag
    */
   public void setResumeValid(boolean resumeValid)
   {
      m_resumeValid = resumeValid;
   }

   /**
    * Retrieve the recurring flag.
    *
    * @return recurring flag
    */
   public boolean getRecurring()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.RECURRING)));
   }

   /**
    * Set the recurring flag.
    *
    * @param recurring recurring flag
    */
   public void setRecurring(boolean recurring)
   {
      set(TaskField.RECURRING, recurring);
   }

   /**
    * Retrieve the over allocated flag.
    *
    * @return over allocated flag
    */
   public boolean getOverAllocated()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.OVERALLOCATED)));
   }

   /**
    * Set the over allocated flag.
    *
    * @param overAllocated over allocated flag
    */
   public void setOverAllocated(boolean overAllocated)
   {
      set(TaskField.OVERALLOCATED, overAllocated);
   }

   /**
    * Where a task in an MPP file represents a task from a subproject,
    * this value will be non-zero. The value itself is the unique ID
    * value shown in the parent project. To retrieve the value of the
    * task unique ID in the child project, remove the top two bytes:
    *
    * taskID = (subprojectUniqueID & 0xFFFF)
    *
    * @return sub project unique task ID
    */
   public Integer getSubprojectTaskUniqueID()
   {
      return (Integer) getCachedValue(TaskField.SUBPROJECT_UNIQUE_TASK_ID);
   }

   /**
    * Sets the sub project unique task ID.
    *
    * @param subprojectUniqueTaskID subproject unique task ID
    */
   public void setSubprojectTaskUniqueID(Integer subprojectUniqueTaskID)
   {
      set(TaskField.SUBPROJECT_UNIQUE_TASK_ID, subprojectUniqueTaskID);
   }

   /**
    * Where a task in an MPP file represents a task from a subproject,
    * this value will be non-zero. The value itself is the ID
    * value shown in the parent project.
    *
    * @return sub project task ID
    */
   public Integer getSubprojectTaskID()
   {
      return (Integer) getCachedValue(TaskField.SUBPROJECT_TASK_ID);
   }

   /**
    * Sets the sub project task ID.
    *
    * @param subprojectTaskID subproject task ID
    */
   public void setSubprojectTaskID(Integer subprojectTaskID)
   {
      set(TaskField.SUBPROJECT_TASK_ID, subprojectTaskID);
   }

   /**
    * Sets the offset added to unique task IDs from sub projects
    * to generate the task ID shown in the master project.
    *
    * @param offset unique ID offset
    */
   public void setSubprojectTasksUniqueIDOffset(Integer offset)
   {
      set(TaskField.SUBPROJECT_TASKS_UNIQUEID_OFFSET, offset);
   }

   /**
    * Retrieves the offset added to unique task IDs from sub projects
    * to generate the task ID shown in the master project.
    *
    * @return unique ID offset
    */
   public Integer getSubprojectTasksUniqueIDOffset()
   {
      return (Integer) getCachedValue(TaskField.SUBPROJECT_TASKS_UNIQUEID_OFFSET);
   }

   /**
    * Retrieve the subproject read only flag.
    *
    * @return subproject read only flag
    */
   public boolean getSubprojectReadOnly()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.SUBPROJECT_READ_ONLY)));
   }

   /**
    * Set the subproject read only flag.
    *
    * @param subprojectReadOnly subproject read only flag
    */
   public void setSubprojectReadOnly(boolean subprojectReadOnly)
   {
      set(TaskField.SUBPROJECT_READ_ONLY, subprojectReadOnly);
   }

   /**
    * Retrieves the external task flag.
    *
    * @return external task flag
    */
   public boolean getExternalTask()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.EXTERNAL_TASK)));
   }

   /**
    * Sets the external task flag.
    *
    * @param externalTask external task flag
    */
   public void setExternalTask(boolean externalTask)
   {
      set(TaskField.EXTERNAL_TASK, externalTask);
   }

   /**
    * Retrieves the external task project file name.
    *
    * @return external task project file name
    */
   public String getExternalTaskProject()
   {
      return (m_externalTaskProject);
   }

   /**
    * Sets the external task project file name.
    *
    * @param externalTaskProject external task project file name
    */
   public void setExternalTaskProject(String externalTaskProject)
   {
      m_externalTaskProject = externalTaskProject;
   }

   /**
    * Retrieve the ACWP value.
    *
    * @return ACWP value
    */
   public Number getACWP()
   {
      return ((Number) getCachedValue(TaskField.ACWP));
   }

   /**
    * Set the ACWP value.
    *
    * @param acwp ACWP value
    */
   public void setACWP(Number acwp)
   {
      set(TaskField.ACWP, acwp);
   }

   /**
    * Retrieve the leveling delay format.
    *
    * @return leveling delay  format
    */
   public TimeUnit getLevelingDelayFormat()
   {
      return (m_levelingDelayFormat);
   }

   /**
    * Set the leveling delay format.
    *
    * @param levelingDelayFormat leveling delay format
    */
   public void setLevelingDelayFormat(TimeUnit levelingDelayFormat)
   {
      m_levelingDelayFormat = levelingDelayFormat;
   }

   /**
    * Retrieves the ignore resource celandar flag.
    *
    * @return ignore resource celandar flag
    */
   public boolean getIgnoreResourceCalendar()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.IGNORE_RESOURCE_CALENDAR)));
   }

   /**
    * Sets the ignore resource celandar flag.
    *
    * @param ignoreResourceCalendar ignore resource celandar flag
    */
   public void setIgnoreResourceCalendar(boolean ignoreResourceCalendar)
   {
      set(TaskField.IGNORE_RESOURCE_CALENDAR, ignoreResourceCalendar);
   }

   /**
    * Retrieves the physical percent complete value.
    *
    * @return physical percent complete value
    */
   public Integer getPhysicalPercentComplete()
   {
      return (Integer) getCachedValue(TaskField.PHYSICAL_PERCENT_COMPLETE);
   }

   /**
    * Srts the physical percent complete value.
    *
    * @param physicalPercentComplete physical percent complete value
    */
   public void setPhysicalPercentComplete(Integer physicalPercentComplete)
   {
      set(TaskField.PHYSICAL_PERCENT_COMPLETE, physicalPercentComplete);
   }

   /**
    * Retrieves the earned value method.
    *
    * @return earned value method
    */
   public EarnedValueMethod getEarnedValueMethod()
   {
      return (m_earnedValueMethod);
   }

   /**
    * Sets the earned value method.
    *
    * @param earnedValueMethod earned value method
    */
   public void setEarnedValueMethod(EarnedValueMethod earnedValueMethod)
   {
      m_earnedValueMethod = earnedValueMethod;
   }

   /**
    * Retrieves the actual work protected value.
    *
    * @return actual work protected value
    */
   public Duration getActualWorkProtected()
   {
      return (m_actualWorkProtected);
   }

   /**
    * Sets the actual work protected value.
    *
    * @param actualWorkProtected actual work protected value
    */
   public void setActualWorkProtected(Duration actualWorkProtected)
   {
      m_actualWorkProtected = actualWorkProtected;
   }

   /**
    * Retrieves the actual overtime work protected value.
    *
    * @return actual overtime work protected value
    */
   public Duration getActualOvertimeWorkProtected()
   {
      return (m_actualOvertimeWorkProtected);
   }

   /**
    * Sets the actual overtime work protected value.
    *
    * @param actualOvertimeWorkProtected actual overtime work protected value
    */
   public void setActualOvertimeWorkProtected(Duration actualOvertimeWorkProtected)
   {
      m_actualOvertimeWorkProtected = actualOvertimeWorkProtected;
   }

   /**
    * Retrieve the amount of regular work.
    *
    * @return amount of regular work
    */
   public Duration getRegularWork()
   {
      return ((Duration) getCachedValue(TaskField.REGULAR_WORK));
   }

   /**
    * Set the amount of regular work.
    *
    * @param regularWork amount of regular work
    */
   public void setRegularWork(Duration regularWork)
   {
      set(TaskField.REGULAR_WORK, regularWork);
   }

   /**
    * Sets the effort driven flag.
    *
    * @param flag value
    */
   public void setEffortDriven(boolean flag)
   {
      set(TaskField.EFFORT_DRIVEN, flag);
   }

   /**
    * Retrieves the effort driven flag.
    *
    * @return Flag value
    */
   public boolean getEffortDriven()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.EFFORT_DRIVEN)));
   }

   /**
    * Set a date value.
    * 
    * @param index date index (1-10)
    * @param value date value
    */
   public void setDate(int index, Date value)
   {
      set(selectField(CUSTOM_DATE, index), value);
   }

   /**
    * Retrieve a date value.
    * 
    * @param index date index (1-10)
    * @return date value
    */
   public Date getDate(int index)
   {
      return (Date) getCachedValue(selectField(CUSTOM_DATE, index));
   }

   /**
    * Retrieves the overtime cost.
    *
    * @return Cost value
    */
   public Number getOvertimeCost()
   {
      return ((Number) getCachedValue(TaskField.OVERTIME_COST));
   }

   /**
    * Sets the overtime cost value.
    *
    * @param number Cost value
    */
   public void setOvertimeCost(Number number)
   {
      set(TaskField.OVERTIME_COST, number);
   }

   /**
    * Retrieves the actual overtime cost for this task.
    *
    * @return actual overtime cost
    */
   public Number getActualOvertimeCost()
   {
      return ((Number) getCachedValue(TaskField.ACTUAL_OVERTIME_COST));
   }

   /**
    * Sets the actual overtime cost for this task.
    *
    * @param cost actual overtime cost
    */
   public void setActualOvertimeCost(Number cost)
   {
      set(TaskField.ACTUAL_OVERTIME_COST, cost);
   }

   /**
    * Retrieves the actual overtime work value.
    *
    * @return actual overtime work value
    */
   public Duration getActualOvertimeWork()
   {
      return ((Duration) getCachedValue(TaskField.ACTUAL_OVERTIME_WORK));
   }

   /**
    * Sets the actual overtime work value.
    *
    * @param work actual overtime work value
    */
   public void setActualOvertimeWork(Duration work)
   {
      set(TaskField.ACTUAL_OVERTIME_WORK, work);
   }

   /**
    * Retrieves the fixed cost accrual flag value.
    *
    * @return fixed cost accrual flag
    */
   public AccrueType getFixedCostAccrual()
   {
      return ((AccrueType) getCachedValue(TaskField.FIXED_COST_ACCRUAL));
   }

   /**
    * Sets the fixed cost accrual flag value.
    *
    * @param type fixed cost accrual type
    */
   public void setFixedCostAccrual(AccrueType type)
   {
      set(TaskField.FIXED_COST_ACCRUAL, type);
   }

   /**
    * Retrieves the task hyperlink attribute.
    *
    * @return hyperlink attribute
    */
   public String getHyperlink()
   {
      return ((String) getCachedValue(TaskField.HYPERLINK));
   }

   /**
    * Retrieves the task hyperlink address attribute.
    *
    * @return hyperlink address attribute
    */
   public String getHyperlinkAddress()
   {
      return ((String) getCachedValue(TaskField.HYPERLINK_ADDRESS));
   }

   /**
    * Retrieves the task hyperlink sub-address attribute.
    *
    * @return hyperlink sub address attribute
    */
   public String getHyperlinkSubAddress()
   {
      return ((String) getCachedValue(TaskField.HYPERLINK_SUBADDRESS));
   }

   /**
    * Sets the task hyperlink attribute.
    *
    * @param text hyperlink attribute
    */
   public void setHyperlink(String text)
   {
      set(TaskField.HYPERLINK, text);
   }

   /**
    * Sets the task hyperlink address attribute.
    *
    * @param text hyperlink address attribute
    */
   public void setHyperlinkAddress(String text)
   {
      set(TaskField.HYPERLINK_ADDRESS, text);
   }

   /**
    * Sets the task hyperlink sub address attribute.
    *
    * @param text hyperlink sub address attribute
    */
   public void setHyperlinkSubAddress(String text)
   {
      set(TaskField.HYPERLINK_SUBADDRESS, text);
   }

   /**
    * Retrieves the level assignments flag.
    *
    * @return level assignments flag
    */
   public boolean getLevelAssignments()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.LEVEL_ASSIGNMENTS)));
   }

   /**
    * Sets the level assignments flag.
    *
    * @param flag level assignments flag
    */
   public void setLevelAssignments(boolean flag)
   {
      set(TaskField.LEVEL_ASSIGNMENTS, flag);
   }

   /**
    * Retrieves the leveling can split flag.
    *
    * @return leveling can split flag
    */
   public boolean getLevelingCanSplit()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.LEVELING_CAN_SPLIT)));
   }

   /**
    * Sets the leveling can split flag.
    *
    * @param flag leveling can split flag
    */
   public void setLevelingCanSplit(boolean flag)
   {
      set(TaskField.LEVELING_CAN_SPLIT, flag);
   }

   /**
    * Retrieves the overtime work attribute.
    *
    * @return overtime work value
    */
   public Duration getOvertimeWork()
   {
      return ((Duration) getCachedValue(TaskField.OVERTIME_WORK));
   }

   /**
    * Sets the overtime work attribute.
    *
    * @param work overtime work value
    */
   public void setOvertimeWork(Duration work)
   {
      set(TaskField.OVERTIME_WORK, work);
   }

   /**
    * Retrieves the preleveled start attribute.
    *
    * @return preleveled start
    */
   public Date getPreleveledStart()
   {
      return ((Date) getCachedValue(TaskField.PRELEVELED_START));
   }

   /**
    * Retrieves the preleveled finish attribute.
    *
    * @return preleveled finish
    */
   public Date getPreleveledFinish()
   {
      return ((Date) getCachedValue(TaskField.PRELEVELED_FINISH));
   }

   /**
    * Sets the preleveled start attribute.
    *
    * @param date preleveled start attribute
    */
   public void setPreleveledStart(Date date)
   {
      set(TaskField.PRELEVELED_START, date);
   }

   /**
    * Sets the preleveled finish attribute.
    *
    * @param date preleveled finish attribute
    */
   public void setPreleveledFinish(Date date)
   {
      set(TaskField.PRELEVELED_FINISH, date);
   }

   /**
    * Retrieves the remaining overtime work attribute.
    *
    * @return remaining overtime work
    */
   public Duration getRemainingOvertimeWork()
   {
      return ((Duration) getCachedValue(TaskField.REMAINING_OVERTIME_WORK));
   }

   /**
    * Sets the remaining overtime work attribute.
    *
    * @param work remaining overtime work
    */
   public void setRemainingOvertimeWork(Duration work)
   {
      set(TaskField.REMAINING_OVERTIME_WORK, work);
   }

   /**
    * Retrieves the remaining overtime cost.
    *
    * @return remaining overtime cost value
    */
   public Number getRemainingOvertimeCost()
   {
      return ((Number) getCachedValue(TaskField.REMAINING_OVERTIME_COST));
   }

   /**
    * Sets the remaining overtime cost value.
    *
    * @param cost overtime cost value
    */
   public void setRemainingOvertimeCost(Number cost)
   {
      set(TaskField.REMAINING_OVERTIME_COST, cost);
   }

   /**
    * Retrieves the base calendar instance associated with this task.
    * Note that this attribute appears in MPP9 and MSPDI files.
    *
    * @return ProjectCalendar instance
    */
   public ProjectCalendar getCalendar()
   {
      return ((ProjectCalendar) getCachedValue(TaskField.CALENDAR));
   }

   /**
    * Sets the name of the base calendar associated with this task.
    * Note that this attribute appears in MPP9 and MSPDI files.
    *
    * @param calendar calendar instance
    */
   public void setCalendar(ProjectCalendar calendar)
   {
      set(TaskField.CALENDAR, calendar);
   }

   /**
    * Retrieve a flag indicating if the task is shown as expanded
    * in MS Project. If this flag is set to true, any sub tasks
    * for this current task will be visible. If this is false,
    * any sub tasks will be hidden.
    *
    * @return boolean flag
    */
   public boolean getExpanded()
   {
      return (m_expanded);
   }

   /**
    * Set a flag indicating if the task is shown as expanded
    * in MS Project. If this flag is set to true, any sub tasks
    * for this current task will be visible. If this is false,
    * any sub tasks will be hidden.
    *
    * @param expanded boolean flag
    */
   public void setExpanded(boolean expanded)
   {
      m_expanded = expanded;
   }

   /**
    * Set the start slack.
    * 
    * @param duration start slack
    */
   public void setStartSlack(Duration duration)
   {
      set(TaskField.START_SLACK, duration);
   }

   /**
    * Set the finish slack.
    * 
    * @param duration finish slack
    */
   public void setFinishSlack(Duration duration)
   {
      set(TaskField.FINISH_SLACK, duration);
   }

   /**
    * Retrieve the start slack.
    * 
    * @return start slack
    */
   public Duration getStartSlack()
   {
      Duration startSlack = (Duration) getCachedValue(TaskField.START_SLACK);
      if (startSlack == null)
      {
         Duration duration = getDuration();
         if (duration != null)
         {
            startSlack = DateUtility.getVariance(this, getLateStart(), getEarlyStart(), duration.getUnits());
            set(TaskField.START_SLACK, startSlack);
         }
      }
      return (startSlack);
   }

   /**
    * Retrieve the finish slack.
    * 
    * @return finish slack
    */
   public Duration getFinishSlack()
   {
      Duration finishSlack = (Duration) getCachedValue(TaskField.FINISH_SLACK);
      if (finishSlack == null)
      {
         Duration duration = getDuration();
         if (duration != null)
         {
            finishSlack = DateUtility.getVariance(this, getLateFinish(), getEarlyFinish(), duration.getUnits());
            set(TaskField.FINISH_SLACK, finishSlack);
         }
      }
      return (finishSlack);
   }

   /**
    * Retrieve the value of a field using its alias.
    *
    * @param alias field alias
    * @return field value
    */
   public Object getFieldByAlias(String alias)
   {
      return (getCachedValue(getParentFile().getAliasTaskField(alias)));
   }

   /**
    * Set the value of a field using its alias.
    *
    * @param alias field alias
    * @param value field value
    */
   public void setFieldByAlias(String alias, Object value)
   {
      set(getParentFile().getAliasTaskField(alias), value);
   }

   /**
    * This method retrieves a list of task splits. Each split is represented
    * by a DateRange instance. The list will always follow the pattern
    * task range, split range, task range and so on.
    *
    * Note that this method will return null if the task is not split.
    *
    * @return list of split times
    */
   public List<DateRange> getSplits()
   {
      return (m_splits);
   }

   /**
    * Internal method used to set the list of splits.
    *
    * @param splits list of split times
    */
   public void setSplits(List<DateRange> splits)
   {
      m_splits = splits;
   }

   /**
    * Task splits contain the time up to which the splits are completed.
    *
    * @return Duration of completed time for the splits.
    */
   public Date getSplitCompleteDuration()
   {
      return m_splitsComplete;
   }

   /**
    * Set the time up to which the splits are completed.
    *
    * @param splitsComplete Duration of completed time for the splits.
    */
   public void setSplitCompleteDuration(Date splitsComplete)
   {
      m_splitsComplete = splitsComplete;
   }

   /**
    * Removes this task from the project.
    */
   public void remove()
   {
      getParentFile().removeTask(this);
   }

   /**
    * Retrieve the sub project represented by this task.
    *
    * @return sub project
    */
   public SubProject getSubProject()
   {
      return (m_subProject);
   }

   /**
    * Set the sub project represented by this task.
    *
    * @param subProject sub project
    */
   public void setSubProject(SubProject subProject)
   {
      m_subProject = subProject;
   }

   /**
    * Retrieve an enterprise field value.
    * 
    * @param index field index
    * @return field value
    */
   public Number getEnterpriseCost(int index)
   {
      return ((Number) getCachedValue(selectField(ENTERPRISE_COST, index)));
   }

   /**
    * Set an enterprise field value.
    * 
    * @param index field index
    * @param value field value
    */
   public void setEnterpriseCost(int index, Number value)
   {
      set(selectField(ENTERPRISE_COST, index), value);
   }

   /**
    * Retrieve an enterprise field value.
    * 
    * @param index field index
    * @return field value
    */
   public Date getEnterpriseDate(int index)
   {
      return ((Date) getCachedValue(selectField(ENTERPRISE_DATE, index)));
   }

   /**
    * Set an enterprise field value.
    * 
    * @param index field index
    * @param value field value
    */
   public void setEnterpriseDate(int index, Date value)
   {
      set(selectField(ENTERPRISE_DATE, index), value);
   }

   /**
    * Retrieve an enterprise field value.
    * 
    * @param index field index
    * @return field value
    */
   public Duration getEnterpriseDuration(int index)
   {
      return ((Duration) getCachedValue(selectField(ENTERPRISE_DURATION, index)));
   }

   /**
    * Set an enterprise field value.
    * 
    * @param index field index
    * @param value field value
    */
   public void setEnterpriseDuration(int index, Duration value)
   {
      set(selectField(ENTERPRISE_DURATION, index), value);
   }

   /**
    * Retrieve an enterprise field value.
    * 
    * @param index field index
    * @return field value
    */
   public boolean getEnterpriseFlag(int index)
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(selectField(ENTERPRISE_FLAG, index))));
   }

   /**
    * Set an enterprise field value.
    * 
    * @param index field index
    * @param value field value
    */
   public void setEnterpriseFlag(int index, boolean value)
   {
      set(selectField(ENTERPRISE_FLAG, index), value);
   }

   /**
    * Retrieve an enterprise field value.
    * 
    * @param index field index
    * @return field value
    */
   public Number getEnterpriseNumber(int index)
   {
      return ((Number) getCachedValue(selectField(ENTERPRISE_NUMBER, index)));
   }

   /**
    * Set an enterprise field value.
    * 
    * @param index field index
    * @param value field value
    */
   public void setEnterpriseNumber(int index, Number value)
   {
      set(selectField(ENTERPRISE_NUMBER, index), value);
   }

   /**
    * Retrieve an enterprise field value.
    * 
    * @param index field index
    * @return field value
    */
   public String getEnterpriseText(int index)
   {
      return ((String) getCachedValue(selectField(ENTERPRISE_TEXT, index)));
   }

   /**
    * Set an enterprise field value.
    * 
    * @param index field index
    * @param value field value
    */
   public void setEnterpriseText(int index, String value)
   {
      set(selectField(ENTERPRISE_TEXT, index), value);
   }

   /**
    * Retrieve an enterprise custom field value.
    * 
    * @param index field index
    * @return field value
    */
   public String getEnterpriseCustomField(int index)
   {
      return ((String) getCachedValue(selectField(ENTERPRISE_CUSTOM_FIELD, index)));
   }

   /**
    * Set an enterprise custom field value.
    * 
    * @param index field index
    * @param value field value
    */
   public void setEnterpriseCustomField(int index, String value)
   {
      set(selectField(ENTERPRISE_CUSTOM_FIELD, index), value);
   }

   /**
    * Set a baseline value.
    * 
    * @param baselineNumber baseline index (1-10)
    * @param value baseline value
    */
   public void setBaselineCost(int baselineNumber, Number value)
   {
      set(selectField(BASELINE_COSTS, baselineNumber), value);
   }

   /**
    * Set a baseline value.
    * 
    * @param baselineNumber baseline index (1-10)
    * @param value baseline value
    */
   public void setBaselineDuration(int baselineNumber, Duration value)
   {
      set(selectField(BASELINE_DURATIONS, baselineNumber), value);
   }

   /**
    * Set a baseline value.
    * 
    * @param baselineNumber baseline index (1-10)
    * @param value baseline value
    */
   public void setBaselineFinish(int baselineNumber, Date value)
   {
      set(selectField(BASELINE_FINISHES, baselineNumber), value);
   }

   /**
    * Set a baseline value.
    * 
    * @param baselineNumber baseline index (1-10)
    * @param value baseline value
    */
   public void setBaselineStart(int baselineNumber, Date value)
   {
      set(selectField(BASELINE_STARTS, baselineNumber), value);
   }

   /**
    * Set a baseline value.
    * 
    * @param baselineNumber baseline index (1-10)
    * @param value baseline value
    */
   public void setBaselineWork(int baselineNumber, Duration value)
   {
      set(selectField(BASELINE_WORKS, baselineNumber), value);
   }

   /**
    * Retrieve a baseline value.
    * 
    * @param baselineNumber baseline index (1-10)
    * @return baseline value
    */
   public Number getBaselineCost(int baselineNumber)
   {
      return ((Number) getCachedValue(selectField(BASELINE_COSTS, baselineNumber)));
   }

   /**
    * Retrieve a baseline value.
    * 
    * @param baselineNumber baseline index (1-10)
    * @return baseline value
    */
   public Duration getBaselineDuration(int baselineNumber)
   {
      Object result = getCachedValue(selectField(BASELINE_DURATIONS, baselineNumber));
      if (result == null)
      {
         result = getCachedValue(selectField(BASELINE_ESTIMATED_DURATIONS, baselineNumber));
      }

      if (!(result instanceof Duration))
      {
         result = null;
      }
      return (Duration) result;
   }

   /**
    * Retrieves the baseline duration text value.
    * 
    * @param baselineNumber baseline number
    * @return baseline duration text value
    */
   public String getBaselineDurationText(int baselineNumber)
   {
      Object result = getCachedValue(selectField(BASELINE_DURATIONS, baselineNumber));
      if (result == null)
      {
         result = getCachedValue(selectField(BASELINE_ESTIMATED_DURATIONS, baselineNumber));
      }

      if (!(result instanceof String))
      {
         result = null;
      }
      return (String) result;
   }

   /**
    * Sets the baseline duration text value.
    * 
    * @param baselineNumber baseline number
    * @param value baseline duration text value
    */
   public void setBaselineDurationText(int baselineNumber, String value)
   {
      set(selectField(BASELINE_DURATIONS, baselineNumber), value);
   }

   /**
    * Retrieve a baseline value.
    * 
    * @param baselineNumber baseline index (1-10)
    * @return baseline value
    */
   public Date getBaselineFinish(int baselineNumber)
   {
      Object result = getCachedValue(selectField(BASELINE_FINISHES, baselineNumber));
      if (result == null)
      {
         result = getCachedValue(selectField(BASELINE_ESTIMATED_FINISHES, baselineNumber));
      }

      if (!(result instanceof Date))
      {
         result = null;
      }
      return (Date) result;
   }

   /**
    * Retrieves the baseline finish text value.
    * 
    * @param baselineNumber baseline number
    * @return baseline finish text value
    */
   public String getBaselineFinishText(int baselineNumber)
   {
      Object result = getCachedValue(selectField(BASELINE_FINISHES, baselineNumber));
      if (result == null)
      {
         result = getCachedValue(selectField(BASELINE_ESTIMATED_FINISHES, baselineNumber));
      }

      if (!(result instanceof String))
      {
         result = null;
      }
      return (String) result;
   }

   /**
    * Sets the baseline finish text value.
    * 
    * @param baselineNumber baseline number
    * @param value baseline finish text value
    */
   public void setBaselineFinishText(int baselineNumber, String value)
   {
      set(selectField(BASELINE_FINISHES, baselineNumber), value);
   }

   /**
    * Retrieve a baseline value.
    * 
    * @param baselineNumber baseline index (1-10)
    * @return baseline value
    */
   public Date getBaselineStart(int baselineNumber)
   {
      Object result = getCachedValue(selectField(BASELINE_STARTS, baselineNumber));
      if (result == null)
      {
         result = getCachedValue(selectField(BASELINE_ESTIMATED_STARTS, baselineNumber));
      }

      if (!(result instanceof Date))
      {
         result = null;
      }
      return (Date) result;
   }

   /**
    * Retrieves the baseline start text value.
    * 
    * @param baselineNumber baseline number
    * @return baseline start text value
    */
   public String getBaselineStartText(int baselineNumber)
   {
      Object result = getCachedValue(selectField(BASELINE_STARTS, baselineNumber));
      if (result == null)
      {
         result = getCachedValue(selectField(BASELINE_ESTIMATED_STARTS, baselineNumber));
      }

      if (!(result instanceof String))
      {
         result = null;
      }
      return (String) result;
   }

   /**
    * Sets the baseline start text value.
    * 
    * @param baselineNumber baseline number
    * @param value baseline start text value
    */
   public void setBaselineStartText(int baselineNumber, String value)
   {
      set(selectField(BASELINE_STARTS, baselineNumber), value);
   }

   /**
    * Retrieve a baseline value.
    * 
    * @param baselineNumber baseline index (1-10)
    * @return baseline value
    */
   public Duration getBaselineWork(int baselineNumber)
   {
      return ((Duration) getCachedValue(selectField(BASELINE_WORKS, baselineNumber)));
   }

   /**
    * Retrieve the "complete through" date.
    * 
    * @return complete through date
    */
   public Date getCompleteThrough()
   {
      Date value = (Date) getCachedValue(TaskField.COMPLETE_THROUGH);
      if (value == null)
      {
         int percentComplete = NumberUtility.getInt(getPercentageComplete());
         switch (percentComplete)
         {
            case 0 :
            {
               break;
            }

            case 100 :
            {
               value = getActualFinish();
               break;
            }

            default :
            {
               Duration duration = getDuration();
               double durationValue = (duration.getDuration() * percentComplete) / 100d;
               duration = Duration.getInstance(durationValue, duration.getUnits());
               ProjectCalendar calendar = getCalendar();
               if (calendar == null)
               {
                  calendar = getParentFile().getCalendar();
               }
               value = calendar.getDate(getActualStart(), duration, true);
               break;
            }
         }

         set(TaskField.COMPLETE_THROUGH, value);
      }
      return value;
   }

   /**
    * Retrieve the summary progress date.
    * 
    * @return summary progress date
    */
   public Date getSummaryProgress()
   {
      Date value = (Date) getCachedValue(TaskField.SUMMARY_PROGRESS);
      return value;
   }

   /**
    * Set the summary progress date.
    * 
    * @param value summary progress date
    */
   public void setSummaryProgress(Date value)
   {
      set(TaskField.SUMMARY_PROGRESS, value);
   }

   /**
    * Retrieve the task GUID.
    * 
    * @return task GUID
    */
   public UUID getGUID()
   {
      return (UUID) getCachedValue(TaskField.GUID);
   }

   /**
    * Set the task GUID.
    * 
    * @param value task GUID
    */
   public void setGUID(UUID value)
   {
      set(TaskField.GUID, value);
   }

   /**
    * Retrieves the task mode.
    * 
    * @return task mode
    */
   public TaskMode getTaskMode()
   {
      return (TaskMode) getCachedValue(TaskField.TASK_MODE);
   }

   /**
    * Sets the task mode.
    * 
    * @param mode task mode
    */
   public void setTaskMode(TaskMode mode)
   {
      set(TaskField.TASK_MODE, mode);
   }

   /**
    * Retrieves the active flag.
    * 
    * @return active flag value
    */
   public boolean getActive()
   {
      return (BooleanUtility.getBoolean((Boolean) getCachedValue(TaskField.ACTIVE)));
   }

   /**
    * Sets the active flag.
    * 
    * @param active active flag value
    */
   public void setActive(boolean active)
   {
      set(TaskField.ACTIVE, active);
   }

   /**
    * This method allows a predecessor relationship to be removed from this
    * task instance.  It will only delete relationships that exactly match the 
    * given targetTask, type and lag time.
    *
    * @param targetTask the predecessor task
    * @param type relation type
    * @param lag relation lag
    * @return returns true if the relation is found and removed
    */
   public boolean removePredecessor(Task targetTask, RelationType type, Duration lag)
   {
      boolean matchFound = false;

      //
      // Retrieve the list of predecessors
      //
      List<Relation> predecessorList = getPredecessors();
      if (predecessorList != null && !predecessorList.isEmpty())
      {
         //
         // Ensure that we have a valid lag duration
         //
         if (lag == null)
         {
            lag = Duration.getInstance(0, TimeUnit.DAYS);
         }

         //
         // Ensure that there is a predecessor relationship between
         // these two tasks, and remove it.
         //
         matchFound = removeRelation(predecessorList, targetTask, type, lag);

         //
         // If we have removed a predecessor, then we must remove the
         // corresponding successor entry from the target task list
         //
         if (matchFound)
         {
            //
            // Retrieve the list of successors
            //
            List<Relation> successorList = targetTask.getSuccessors();
            if (successorList != null && !successorList.isEmpty())
            {
               //
               // Ensure that there is a successor relationship between
               // these two tasks, and remove it.
               //
               removeRelation(successorList, this, type, lag);
            }
         }
      }

      return matchFound;
   }

   /**
    * Internal method used to locate an remove an item from a list Relations. 
    * 
    * @param relationList list of Relation instances
    * @param targetTask target relationship task
    * @param type target relationship type
    * @param lag target relationship lag
    * @return true if a relationship was removed
    */
   private boolean removeRelation(List<Relation> relationList, Task targetTask, RelationType type, Duration lag)
   {
      boolean matchFound = false;
      for (Relation relation : relationList)
      {
         if (relation.getTargetTask() == targetTask)
         {
            if (relation.getType() == type && relation.getLag().compareTo(lag) == 0)
            {
               matchFound = relationList.remove(relation);
               break;
            }
         }
      }
      return matchFound;
   }

   /**
    * Maps a field index to a TaskField instance.
    * 
    * @param fields array of fields used as the basis for the mapping.
    * @param index required field index
    * @return TaskField instance
    */
   private TaskField selectField(TaskField[] fields, int index)
   {
      if (index < 1 || index > fields.length)
      {
         throw new IllegalArgumentException(index + " is not a valid field index");
      }
      return (fields[index - 1]);
   }

   /**
    * {@inheritDoc}
    */
   @Override public Object getCachedValue(FieldType field)
   {
      return (field == null ? null : m_array[field.getValue()]);
   }

   /**
    * {@inheritDoc}
    */
   @Override public Object getCurrentValue(FieldType field)
   {
      Object result = null;

      if (field != null)
      {
         switch ((TaskField) field)
         {
            case START_VARIANCE :
            {
               result = getStartVariance();
               break;
            }

            case COST_VARIANCE :
            {
               result = getCostVariance();
               break;
            }

            case DURATION_VARIANCE :
            {
               result = getDurationVariance();
               break;
            }

            case WORK_VARIANCE :
            {
               result = getWorkVariance();
               break;
            }

            case CV :
            {
               result = getCV();
               break;
            }

            case SV :
            {
               result = getSV();
               break;
            }

            case TOTAL_SLACK :
            {
               result = getTotalSlack();
               break;
            }

            case CRITICAL :
            {
               result = Boolean.valueOf(getCritical());
               break;
            }

            case COMPLETE_THROUGH :
            {
               result = getCompleteThrough();
               break;
            }

            default :
            {
               result = m_array[field.getValue()];
               break;
            }
         }
      }

      return (result);
   }

   /**
    * {@inheritDoc}
    */
   @Override public void set(FieldType field, Object value)
   {
      if (field != null)
      {
         int index = field.getValue();
         if (m_eventsEnabled)
         {
            fireFieldChangeEvent((TaskField) field, m_array[index], value);
         }
         m_array[index] = value;
      }
   }

   /**
    * Handle the change in a field value. Reset any cached calculated
    * values affected by this change, pass on the event to any external
    * listeners.
    * 
    * @param field field changed
    * @param oldValue old field value
    * @param newValue new field value
    */
   private void fireFieldChangeEvent(TaskField field, Object oldValue, Object newValue)
   {
      //
      // Internal event handling
      //
      switch (field)
      {
         case START :
         case BASELINE_START :
         {
            m_array[TaskField.START_VARIANCE.getValue()] = null;
            break;
         }

         case FINISH :
         case BASELINE_FINISH :
         {
            m_array[TaskField.FINISH_VARIANCE.getValue()] = null;
            break;
         }

         case COST :
         case BASELINE_COST :
         {
            m_array[TaskField.COST_VARIANCE.getValue()] = null;
            break;
         }

         case DURATION :
         {
            m_array[TaskField.DURATION_VARIANCE.getValue()] = null;
            m_array[TaskField.COMPLETE_THROUGH.getValue()] = null;
            break;
         }

         case BASELINE_DURATION :
         {
            m_array[TaskField.DURATION_VARIANCE.getValue()] = null;
            break;
         }

         case WORK :
         case BASELINE_WORK :
         {
            m_array[TaskField.WORK_VARIANCE.getValue()] = null;
            break;
         }

         case BCWP :
         case ACWP :
         {
            m_array[TaskField.CV.getValue()] = null;
            m_array[TaskField.SV.getValue()] = null;
            break;
         }

         case BCWS :
         {
            m_array[TaskField.SV.getValue()] = null;
            break;
         }

         case START_SLACK :
         case FINISH_SLACK :
         {
            m_array[TaskField.TOTAL_SLACK.getValue()] = null;
            m_array[TaskField.CRITICAL.getValue()] = null;
            break;
         }

         case EARLY_FINISH :
         case LATE_FINISH :
         {
            m_array[TaskField.FINISH_SLACK.getValue()] = null;
            m_array[TaskField.TOTAL_SLACK.getValue()] = null;
            m_array[TaskField.CRITICAL.getValue()] = null;
            break;
         }

         case EARLY_START :
         case LATE_START :
         {
            m_array[TaskField.START_SLACK.getValue()] = null;
            m_array[TaskField.TOTAL_SLACK.getValue()] = null;
            m_array[TaskField.CRITICAL.getValue()] = null;
            break;
         }

         case ACTUAL_START :
         case PERCENT_COMPLETE :
         {
            m_array[TaskField.COMPLETE_THROUGH.getValue()] = null;
            break;
         }

         default :
         {
            break;
         }
      }

      //
      // External event handling
      //
      if (m_listeners != null)
      {
         for (FieldListener listener : m_listeners)
         {
            listener.fieldChange(this, field, oldValue, newValue);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override public void addFieldListener(FieldListener listener)
   {
      if (m_listeners == null)
      {
         m_listeners = new LinkedList<FieldListener>();
      }
      m_listeners.add(listener);
   }

   /**
    * {@inheritDoc}
    */
   @Override public void removeFieldListener(FieldListener listener)
   {
      if (m_listeners != null)
      {
         m_listeners.remove(listener);
      }
   }

   /**
    * This method inserts a name value pair into internal storage.
    *
    * @param field task field
    * @param value attribute value
    */
   private void set(FieldType field, boolean value)
   {
      set(field, (value ? Boolean.TRUE : Boolean.FALSE));
   }

   /**
    * {@inheritDoc}
    */
   @Override public String toString()
   {
      return ("[Task id=" + getID() + " uniqueID=" + getUniqueID() + " name=" + getName() + (getExternalTask() ? " [EXTERNAL uid=" + getSubprojectTaskUniqueID() + " id=" + getSubprojectTaskID() + "]" : "]") + (getSubProject() == null ? "" : (" project=" + getSubProject())));
   }

   /**
    * Utility method used to determine if the supplied task
    * is a predecessor of the current task.
    * 
    * @param task potential predecessor task
    * @return Boolean flag
    */
   public boolean isPredecessor(Task task)
   {
      return isRelated(task, getPredecessors());
   }

   /**
    * Utility method used to determine if the supplied task
    * is a successor of the current task.
    * 
    * @param task potential successor task
    * @return Boolean flag
    */
   public boolean isSucessor(Task task)
   {
      return isRelated(task, getSuccessors());
   }

   /**
    * Internal method used to test for the existence of a relationship
    * with a task.
    * 
    * @param task target task
    * @param list list of relationships
    * @return boolean flag
    */
   private boolean isRelated(Task task, List<Relation> list)
   {
      boolean result = false;
      if (list != null)
      {
         for (Relation relation : list)
         {
            if (relation.getTargetTask().getUniqueID() == task.getUniqueID())
            {
               result = true;
               break;
            }
         }
      }
      return result;
   }

   /**
    * Disable events firing when fields are updated.
    */
   public void disableEvents()
   {
      m_eventsEnabled = false;
   }

   /**
    * Enable events firing when fields are updated. This is the default state.
    */
   public void enableEvents()
   {
      m_eventsEnabled = true;
   }

   /**
    * Array of field values.
    */
   private Object[] m_array = new Object[TaskField.MAX_VALUE];

   /**
    * This is a reference to the parent task, as specified by the
    * outline level.
    */
   private Task m_parent;

   /**
    * This list holds references to all tasks that are children of the
    * current task as specified by the outline level.
    */
   private List<Task> m_children = new LinkedList<Task>();

   /**
    * List of resource assignments for this task.
    */
   private List<ResourceAssignment> m_assignments = new LinkedList<ResourceAssignment>();

   /**
    * Recurring task details associated with this task.
    */
   private RecurringTask m_recurringTask;

   private static final TaskField[] CUSTOM_COST =
   {
      TaskField.COST1,
      TaskField.COST2,
      TaskField.COST3,
      TaskField.COST4,
      TaskField.COST5,
      TaskField.COST6,
      TaskField.COST7,
      TaskField.COST8,
      TaskField.COST9,
      TaskField.COST10
   };

   private static final TaskField[] CUSTOM_DATE =
   {
      TaskField.DATE1,
      TaskField.DATE2,
      TaskField.DATE3,
      TaskField.DATE4,
      TaskField.DATE5,
      TaskField.DATE6,
      TaskField.DATE7,
      TaskField.DATE8,
      TaskField.DATE9,
      TaskField.DATE10
   };

   private static final TaskField[] CUSTOM_DURATION =
   {
      TaskField.DURATION1,
      TaskField.DURATION2,
      TaskField.DURATION3,
      TaskField.DURATION4,
      TaskField.DURATION5,
      TaskField.DURATION6,
      TaskField.DURATION7,
      TaskField.DURATION8,
      TaskField.DURATION9,
      TaskField.DURATION10
   };

   private static final TaskField[] CUSTOM_FLAG =
   {
      TaskField.FLAG1,
      TaskField.FLAG2,
      TaskField.FLAG3,
      TaskField.FLAG4,
      TaskField.FLAG5,
      TaskField.FLAG6,
      TaskField.FLAG7,
      TaskField.FLAG8,
      TaskField.FLAG9,
      TaskField.FLAG10,
      TaskField.FLAG11,
      TaskField.FLAG12,
      TaskField.FLAG13,
      TaskField.FLAG14,
      TaskField.FLAG15,
      TaskField.FLAG16,
      TaskField.FLAG17,
      TaskField.FLAG18,
      TaskField.FLAG19,
      TaskField.FLAG20
   };

   private static final TaskField[] CUSTOM_FINISH =
   {
      TaskField.FINISH1,
      TaskField.FINISH2,
      TaskField.FINISH3,
      TaskField.FINISH4,
      TaskField.FINISH5,
      TaskField.FINISH6,
      TaskField.FINISH7,
      TaskField.FINISH8,
      TaskField.FINISH9,
      TaskField.FINISH10
   };

   private static final TaskField[] CUSTOM_NUMBER =
   {
      TaskField.NUMBER1,
      TaskField.NUMBER2,
      TaskField.NUMBER3,
      TaskField.NUMBER4,
      TaskField.NUMBER5,
      TaskField.NUMBER6,
      TaskField.NUMBER7,
      TaskField.NUMBER8,
      TaskField.NUMBER9,
      TaskField.NUMBER10,
      TaskField.NUMBER11,
      TaskField.NUMBER12,
      TaskField.NUMBER13,
      TaskField.NUMBER14,
      TaskField.NUMBER15,
      TaskField.NUMBER16,
      TaskField.NUMBER17,
      TaskField.NUMBER18,
      TaskField.NUMBER19,
      TaskField.NUMBER20
   };

   private static final TaskField[] CUSTOM_START =
   {
      TaskField.START1,
      TaskField.START2,
      TaskField.START3,
      TaskField.START4,
      TaskField.START5,
      TaskField.START6,
      TaskField.START7,
      TaskField.START8,
      TaskField.START9,
      TaskField.START10
   };

   private static final TaskField[] CUSTOM_TEXT =
   {
      TaskField.TEXT1,
      TaskField.TEXT2,
      TaskField.TEXT3,
      TaskField.TEXT4,
      TaskField.TEXT5,
      TaskField.TEXT6,
      TaskField.TEXT7,
      TaskField.TEXT8,
      TaskField.TEXT9,
      TaskField.TEXT10,
      TaskField.TEXT11,
      TaskField.TEXT12,
      TaskField.TEXT13,
      TaskField.TEXT14,
      TaskField.TEXT15,
      TaskField.TEXT16,
      TaskField.TEXT17,
      TaskField.TEXT18,
      TaskField.TEXT19,
      TaskField.TEXT20,
      TaskField.TEXT21,
      TaskField.TEXT22,
      TaskField.TEXT23,
      TaskField.TEXT24,
      TaskField.TEXT25,
      TaskField.TEXT26,
      TaskField.TEXT27,
      TaskField.TEXT28,
      TaskField.TEXT29,
      TaskField.TEXT30
   };

   private static final TaskField[] CUSTOM_OUTLINE_CODE =
   {
      TaskField.OUTLINE_CODE1,
      TaskField.OUTLINE_CODE2,
      TaskField.OUTLINE_CODE3,
      TaskField.OUTLINE_CODE4,
      TaskField.OUTLINE_CODE5,
      TaskField.OUTLINE_CODE6,
      TaskField.OUTLINE_CODE7,
      TaskField.OUTLINE_CODE8,
      TaskField.OUTLINE_CODE9,
      TaskField.OUTLINE_CODE10
   };

   private static final TaskField[] ENTERPRISE_COST =
   {
      TaskField.ENTERPRISE_COST1,
      TaskField.ENTERPRISE_COST2,
      TaskField.ENTERPRISE_COST3,
      TaskField.ENTERPRISE_COST4,
      TaskField.ENTERPRISE_COST5,
      TaskField.ENTERPRISE_COST6,
      TaskField.ENTERPRISE_COST7,
      TaskField.ENTERPRISE_COST8,
      TaskField.ENTERPRISE_COST9,
      TaskField.ENTERPRISE_COST10
   };

   private static final TaskField[] ENTERPRISE_DATE =
   {
      TaskField.ENTERPRISE_DATE1,
      TaskField.ENTERPRISE_DATE2,
      TaskField.ENTERPRISE_DATE3,
      TaskField.ENTERPRISE_DATE4,
      TaskField.ENTERPRISE_DATE5,
      TaskField.ENTERPRISE_DATE6,
      TaskField.ENTERPRISE_DATE7,
      TaskField.ENTERPRISE_DATE8,
      TaskField.ENTERPRISE_DATE9,
      TaskField.ENTERPRISE_DATE10,
      TaskField.ENTERPRISE_DATE11,
      TaskField.ENTERPRISE_DATE12,
      TaskField.ENTERPRISE_DATE13,
      TaskField.ENTERPRISE_DATE14,
      TaskField.ENTERPRISE_DATE15,
      TaskField.ENTERPRISE_DATE16,
      TaskField.ENTERPRISE_DATE17,
      TaskField.ENTERPRISE_DATE18,
      TaskField.ENTERPRISE_DATE19,
      TaskField.ENTERPRISE_DATE20,
      TaskField.ENTERPRISE_DATE21,
      TaskField.ENTERPRISE_DATE22,
      TaskField.ENTERPRISE_DATE23,
      TaskField.ENTERPRISE_DATE24,
      TaskField.ENTERPRISE_DATE25,
      TaskField.ENTERPRISE_DATE26,
      TaskField.ENTERPRISE_DATE27,
      TaskField.ENTERPRISE_DATE28,
      TaskField.ENTERPRISE_DATE29,
      TaskField.ENTERPRISE_DATE30
   };

   private static final TaskField[] ENTERPRISE_DURATION =
   {
      TaskField.ENTERPRISE_DURATION1,
      TaskField.ENTERPRISE_DURATION2,
      TaskField.ENTERPRISE_DURATION3,
      TaskField.ENTERPRISE_DURATION4,
      TaskField.ENTERPRISE_DURATION5,
      TaskField.ENTERPRISE_DURATION6,
      TaskField.ENTERPRISE_DURATION7,
      TaskField.ENTERPRISE_DURATION8,
      TaskField.ENTERPRISE_DURATION9,
      TaskField.ENTERPRISE_DURATION10
   };

   private static final TaskField[] ENTERPRISE_FLAG =
   {
      TaskField.ENTERPRISE_FLAG1,
      TaskField.ENTERPRISE_FLAG2,
      TaskField.ENTERPRISE_FLAG3,
      TaskField.ENTERPRISE_FLAG4,
      TaskField.ENTERPRISE_FLAG5,
      TaskField.ENTERPRISE_FLAG6,
      TaskField.ENTERPRISE_FLAG7,
      TaskField.ENTERPRISE_FLAG8,
      TaskField.ENTERPRISE_FLAG9,
      TaskField.ENTERPRISE_FLAG10,
      TaskField.ENTERPRISE_FLAG11,
      TaskField.ENTERPRISE_FLAG12,
      TaskField.ENTERPRISE_FLAG13,
      TaskField.ENTERPRISE_FLAG14,
      TaskField.ENTERPRISE_FLAG15,
      TaskField.ENTERPRISE_FLAG16,
      TaskField.ENTERPRISE_FLAG17,
      TaskField.ENTERPRISE_FLAG18,
      TaskField.ENTERPRISE_FLAG19,
      TaskField.ENTERPRISE_FLAG20
   };

   private static final TaskField[] ENTERPRISE_NUMBER =
   {
      TaskField.ENTERPRISE_NUMBER1,
      TaskField.ENTERPRISE_NUMBER2,
      TaskField.ENTERPRISE_NUMBER3,
      TaskField.ENTERPRISE_NUMBER4,
      TaskField.ENTERPRISE_NUMBER5,
      TaskField.ENTERPRISE_NUMBER6,
      TaskField.ENTERPRISE_NUMBER7,
      TaskField.ENTERPRISE_NUMBER8,
      TaskField.ENTERPRISE_NUMBER9,
      TaskField.ENTERPRISE_NUMBER10,
      TaskField.ENTERPRISE_NUMBER11,
      TaskField.ENTERPRISE_NUMBER12,
      TaskField.ENTERPRISE_NUMBER13,
      TaskField.ENTERPRISE_NUMBER14,
      TaskField.ENTERPRISE_NUMBER15,
      TaskField.ENTERPRISE_NUMBER16,
      TaskField.ENTERPRISE_NUMBER17,
      TaskField.ENTERPRISE_NUMBER18,
      TaskField.ENTERPRISE_NUMBER19,
      TaskField.ENTERPRISE_NUMBER20,
      TaskField.ENTERPRISE_NUMBER21,
      TaskField.ENTERPRISE_NUMBER22,
      TaskField.ENTERPRISE_NUMBER23,
      TaskField.ENTERPRISE_NUMBER24,
      TaskField.ENTERPRISE_NUMBER25,
      TaskField.ENTERPRISE_NUMBER26,
      TaskField.ENTERPRISE_NUMBER27,
      TaskField.ENTERPRISE_NUMBER28,
      TaskField.ENTERPRISE_NUMBER29,
      TaskField.ENTERPRISE_NUMBER30,
      TaskField.ENTERPRISE_NUMBER31,
      TaskField.ENTERPRISE_NUMBER32,
      TaskField.ENTERPRISE_NUMBER33,
      TaskField.ENTERPRISE_NUMBER34,
      TaskField.ENTERPRISE_NUMBER35,
      TaskField.ENTERPRISE_NUMBER36,
      TaskField.ENTERPRISE_NUMBER37,
      TaskField.ENTERPRISE_NUMBER38,
      TaskField.ENTERPRISE_NUMBER39,
      TaskField.ENTERPRISE_NUMBER40
   };

   private static final TaskField[] ENTERPRISE_TEXT =
   {
      TaskField.ENTERPRISE_TEXT1,
      TaskField.ENTERPRISE_TEXT2,
      TaskField.ENTERPRISE_TEXT3,
      TaskField.ENTERPRISE_TEXT4,
      TaskField.ENTERPRISE_TEXT5,
      TaskField.ENTERPRISE_TEXT6,
      TaskField.ENTERPRISE_TEXT7,
      TaskField.ENTERPRISE_TEXT8,
      TaskField.ENTERPRISE_TEXT9,
      TaskField.ENTERPRISE_TEXT10,
      TaskField.ENTERPRISE_TEXT11,
      TaskField.ENTERPRISE_TEXT12,
      TaskField.ENTERPRISE_TEXT13,
      TaskField.ENTERPRISE_TEXT14,
      TaskField.ENTERPRISE_TEXT15,
      TaskField.ENTERPRISE_TEXT16,
      TaskField.ENTERPRISE_TEXT17,
      TaskField.ENTERPRISE_TEXT18,
      TaskField.ENTERPRISE_TEXT19,
      TaskField.ENTERPRISE_TEXT20,
      TaskField.ENTERPRISE_TEXT21,
      TaskField.ENTERPRISE_TEXT22,
      TaskField.ENTERPRISE_TEXT23,
      TaskField.ENTERPRISE_TEXT24,
      TaskField.ENTERPRISE_TEXT25,
      TaskField.ENTERPRISE_TEXT26,
      TaskField.ENTERPRISE_TEXT27,
      TaskField.ENTERPRISE_TEXT28,
      TaskField.ENTERPRISE_TEXT29,
      TaskField.ENTERPRISE_TEXT30,
      TaskField.ENTERPRISE_TEXT31,
      TaskField.ENTERPRISE_TEXT32,
      TaskField.ENTERPRISE_TEXT33,
      TaskField.ENTERPRISE_TEXT34,
      TaskField.ENTERPRISE_TEXT35,
      TaskField.ENTERPRISE_TEXT36,
      TaskField.ENTERPRISE_TEXT37,
      TaskField.ENTERPRISE_TEXT38,
      TaskField.ENTERPRISE_TEXT39,
      TaskField.ENTERPRISE_TEXT40
   };

   private static final TaskField[] ENTERPRISE_CUSTOM_FIELD =
   {
      TaskField.ENTERPRISE_CUSTOM_FIELD1,
      TaskField.ENTERPRISE_CUSTOM_FIELD2,
      TaskField.ENTERPRISE_CUSTOM_FIELD3,
      TaskField.ENTERPRISE_CUSTOM_FIELD4,
      TaskField.ENTERPRISE_CUSTOM_FIELD5,
      TaskField.ENTERPRISE_CUSTOM_FIELD6,
      TaskField.ENTERPRISE_CUSTOM_FIELD7,
      TaskField.ENTERPRISE_CUSTOM_FIELD8,
      TaskField.ENTERPRISE_CUSTOM_FIELD9,
      TaskField.ENTERPRISE_CUSTOM_FIELD10,
      TaskField.ENTERPRISE_CUSTOM_FIELD11,
      TaskField.ENTERPRISE_CUSTOM_FIELD12,
      TaskField.ENTERPRISE_CUSTOM_FIELD13,
      TaskField.ENTERPRISE_CUSTOM_FIELD14,
      TaskField.ENTERPRISE_CUSTOM_FIELD15,
      TaskField.ENTERPRISE_CUSTOM_FIELD16,
      TaskField.ENTERPRISE_CUSTOM_FIELD17,
      TaskField.ENTERPRISE_CUSTOM_FIELD18,
      TaskField.ENTERPRISE_CUSTOM_FIELD19,
      TaskField.ENTERPRISE_CUSTOM_FIELD20,
      TaskField.ENTERPRISE_CUSTOM_FIELD21,
      TaskField.ENTERPRISE_CUSTOM_FIELD22,
      TaskField.ENTERPRISE_CUSTOM_FIELD23,
      TaskField.ENTERPRISE_CUSTOM_FIELD24,
      TaskField.ENTERPRISE_CUSTOM_FIELD25,
      TaskField.ENTERPRISE_CUSTOM_FIELD26,
      TaskField.ENTERPRISE_CUSTOM_FIELD27,
      TaskField.ENTERPRISE_CUSTOM_FIELD28,
      TaskField.ENTERPRISE_CUSTOM_FIELD29,
      TaskField.ENTERPRISE_CUSTOM_FIELD30,
      TaskField.ENTERPRISE_CUSTOM_FIELD31,
      TaskField.ENTERPRISE_CUSTOM_FIELD32,
      TaskField.ENTERPRISE_CUSTOM_FIELD33,
      TaskField.ENTERPRISE_CUSTOM_FIELD34,
      TaskField.ENTERPRISE_CUSTOM_FIELD35,
      TaskField.ENTERPRISE_CUSTOM_FIELD36,
      TaskField.ENTERPRISE_CUSTOM_FIELD37,
      TaskField.ENTERPRISE_CUSTOM_FIELD38,
      TaskField.ENTERPRISE_CUSTOM_FIELD39,
      TaskField.ENTERPRISE_CUSTOM_FIELD40,
      TaskField.ENTERPRISE_CUSTOM_FIELD41,
      TaskField.ENTERPRISE_CUSTOM_FIELD42,
      TaskField.ENTERPRISE_CUSTOM_FIELD43,
      TaskField.ENTERPRISE_CUSTOM_FIELD44,
      TaskField.ENTERPRISE_CUSTOM_FIELD45,
      TaskField.ENTERPRISE_CUSTOM_FIELD46,
      TaskField.ENTERPRISE_CUSTOM_FIELD47,
      TaskField.ENTERPRISE_CUSTOM_FIELD48,
      TaskField.ENTERPRISE_CUSTOM_FIELD49,
      TaskField.ENTERPRISE_CUSTOM_FIELD50
   };

   private static final TaskField[] BASELINE_COSTS =
   {
      TaskField.BASELINE1_COST,
      TaskField.BASELINE2_COST,
      TaskField.BASELINE3_COST,
      TaskField.BASELINE4_COST,
      TaskField.BASELINE5_COST,
      TaskField.BASELINE6_COST,
      TaskField.BASELINE7_COST,
      TaskField.BASELINE8_COST,
      TaskField.BASELINE9_COST,
      TaskField.BASELINE10_COST
   };

   private static final TaskField[] BASELINE_DURATIONS =
   {
      TaskField.BASELINE1_DURATION,
      TaskField.BASELINE2_DURATION,
      TaskField.BASELINE3_DURATION,
      TaskField.BASELINE4_DURATION,
      TaskField.BASELINE5_DURATION,
      TaskField.BASELINE6_DURATION,
      TaskField.BASELINE7_DURATION,
      TaskField.BASELINE8_DURATION,
      TaskField.BASELINE9_DURATION,
      TaskField.BASELINE10_DURATION
   };

   private static final TaskField[] BASELINE_ESTIMATED_DURATIONS =
   {
      TaskField.BASELINE1_ESTIMATED_DURATION,
      TaskField.BASELINE2_ESTIMATED_DURATION,
      TaskField.BASELINE3_ESTIMATED_DURATION,
      TaskField.BASELINE4_ESTIMATED_DURATION,
      TaskField.BASELINE5_ESTIMATED_DURATION,
      TaskField.BASELINE6_ESTIMATED_DURATION,
      TaskField.BASELINE7_ESTIMATED_DURATION,
      TaskField.BASELINE8_ESTIMATED_DURATION,
      TaskField.BASELINE9_ESTIMATED_DURATION,
      TaskField.BASELINE10_ESTIMATED_DURATION
   };

   private static final TaskField[] BASELINE_STARTS =
   {
      TaskField.BASELINE1_START,
      TaskField.BASELINE2_START,
      TaskField.BASELINE3_START,
      TaskField.BASELINE4_START,
      TaskField.BASELINE5_START,
      TaskField.BASELINE6_START,
      TaskField.BASELINE7_START,
      TaskField.BASELINE8_START,
      TaskField.BASELINE9_START,
      TaskField.BASELINE10_START
   };

   private static final TaskField[] BASELINE_ESTIMATED_STARTS =
   {
      TaskField.BASELINE1_ESTIMATED_START,
      TaskField.BASELINE2_ESTIMATED_START,
      TaskField.BASELINE3_ESTIMATED_START,
      TaskField.BASELINE4_ESTIMATED_START,
      TaskField.BASELINE5_ESTIMATED_START,
      TaskField.BASELINE6_ESTIMATED_START,
      TaskField.BASELINE7_ESTIMATED_START,
      TaskField.BASELINE8_ESTIMATED_START,
      TaskField.BASELINE9_ESTIMATED_START,
      TaskField.BASELINE10_ESTIMATED_START
   };

   private static final TaskField[] BASELINE_FINISHES =
   {
      TaskField.BASELINE1_FINISH,
      TaskField.BASELINE2_FINISH,
      TaskField.BASELINE3_FINISH,
      TaskField.BASELINE4_FINISH,
      TaskField.BASELINE5_FINISH,
      TaskField.BASELINE6_FINISH,
      TaskField.BASELINE7_FINISH,
      TaskField.BASELINE8_FINISH,
      TaskField.BASELINE9_FINISH,
      TaskField.BASELINE10_FINISH
   };

   private static final TaskField[] BASELINE_ESTIMATED_FINISHES =
   {
      TaskField.BASELINE1_ESTIMATED_FINISH,
      TaskField.BASELINE2_ESTIMATED_FINISH,
      TaskField.BASELINE3_ESTIMATED_FINISH,
      TaskField.BASELINE4_ESTIMATED_FINISH,
      TaskField.BASELINE5_ESTIMATED_FINISH,
      TaskField.BASELINE6_ESTIMATED_FINISH,
      TaskField.BASELINE7_ESTIMATED_FINISH,
      TaskField.BASELINE8_ESTIMATED_FINISH,
      TaskField.BASELINE9_ESTIMATED_FINISH,
      TaskField.BASELINE10_ESTIMATED_FINISH
   };

   private static final TaskField[] BASELINE_WORKS =
   {
      TaskField.BASELINE1_WORK,
      TaskField.BASELINE2_WORK,
      TaskField.BASELINE3_WORK,
      TaskField.BASELINE4_WORK,
      TaskField.BASELINE5_WORK,
      TaskField.BASELINE6_WORK,
      TaskField.BASELINE7_WORK,
      TaskField.BASELINE8_WORK,
      TaskField.BASELINE9_WORK,
      TaskField.BASELINE10_WORK
   };

   private boolean m_eventsEnabled = true;
   private boolean m_null;
   private String m_wbsLevel;
   private boolean m_resumeValid;
   private String m_externalTaskProject;
   private TimeUnit m_levelingDelayFormat;
   private EarnedValueMethod m_earnedValueMethod;
   private Duration m_actualWorkProtected;
   private Duration m_actualOvertimeWorkProtected;
   private boolean m_expanded = true;

   private List<DateRange> m_splits;
   private Date m_splitsComplete;
   private SubProject m_subProject;
   private List<FieldListener> m_listeners;
}

/*
// NEW FIELDS - to be added in MPXJ 5.0
 * VAC

{TaskField.Baseline Budget Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(104)},
{TaskField.Baseline Budget Work, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(103)},
{TaskField.Baseline Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(102)},
{TaskField.Baseline Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(101)},
{TaskField.Baseline Fixed Cost Accrual, FieldLocation.FIXED_DATA, Integer.valueOf(44), Integer.valueOf(0)},
{TaskField.Baseline Fixed Cost, FieldLocation.FIXED_DATA, Integer.valueOf(256), Integer.valueOf(0)},
{TaskField.Baseline1 Budget Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(111)},
{TaskField.Baseline1 Budget Work, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(110)},
{TaskField.Baseline1 Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(109)},
{TaskField.Baseline1 Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(108)},
{TaskField.Baseline1 Fixed Cost Accrual, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(107)},
{TaskField.Baseline1 Fixed Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(170)},
{TaskField.Baseline2 Budget Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(118)},
{TaskField.Baseline2 Budget Work, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(117)},
{TaskField.Baseline2 Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(116)},
{TaskField.Baseline2 Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(115)},
{TaskField.Baseline2 Fixed Cost Accrual, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(114)},
{TaskField.Baseline2 Fixed Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(180)},
{TaskField.Baseline3 Budget Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(125)},
{TaskField.Baseline3 Budget Work, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(124)},
{TaskField.Baseline3 Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(123)},
{TaskField.Baseline3 Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(122)},
{TaskField.Baseline3 Fixed Cost Accrual, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(121)},
{TaskField.Baseline3 Fixed Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(190)},
{TaskField.Baseline4 Budget Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(132)},
{TaskField.Baseline4 Budget Work, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(131)},
{TaskField.Baseline4 Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(130)},
{TaskField.Baseline4 Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(129)},
{TaskField.Baseline4 Fixed Cost Accrual, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(128)},
{TaskField.Baseline4 Fixed Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(200)},
{TaskField.Baseline5 Budget Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(139)},
{TaskField.Baseline5 Budget Work, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(138)},
{TaskField.Baseline5 Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(137)},
{TaskField.Baseline5 Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(136)},
{TaskField.Baseline5 Fixed Cost Accrual, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(135)},
{TaskField.Baseline5 Fixed Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(210)},
{TaskField.Baseline6 Budget Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(146)},
{TaskField.Baseline6 Budget Work, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(145)},
{TaskField.Baseline6 Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(144)},
{TaskField.Baseline6 Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(143)},
{TaskField.Baseline6 Fixed Cost Accrual, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(142)},
{TaskField.Baseline6 Fixed Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(220)},
{TaskField.Baseline7 Budget Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(153)},
{TaskField.Baseline7 Budget Work, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(152)},
{TaskField.Baseline7 Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(151)},
{TaskField.Baseline7 Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(150)},
{TaskField.Baseline7 Fixed Cost Accrual, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(149)},
{TaskField.Baseline7 Fixed Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(230)},
{TaskField.Baseline8 Budget Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(160)},
{TaskField.Baseline8 Budget Work, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(159)},
{TaskField.Baseline8 Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(158)},
{TaskField.Baseline8 Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(157)},
{TaskField.Baseline8 Fixed Cost Accrual, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(156)},
{TaskField.Baseline8 Fixed Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(240)},
{TaskField.Baseline9 Budget Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(167)},
{TaskField.Baseline9 Budget Work, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(166)},
{TaskField.Baseline9 Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(165)},
{TaskField.Baseline9 Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(164)},
{TaskField.Baseline9 Fixed Cost Accrual, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(163)},
{TaskField.Baseline9 Fixed Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(250)},
{TaskField.Baseline10 Budget Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(174)},
{TaskField.Baseline10 Budget Work, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(173)},
{TaskField.Baseline10 Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(172)},
{TaskField.Baseline10 Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(171)},
{TaskField.Baseline10 Fixed Cost Accrual, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(170)},
{TaskField.Baseline10 Fixed Cost, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(4)},
{TaskField.Deliverable Finish, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(180)},
{TaskField.Deliverable GUID, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(177)},
{TaskField.Deliverable Name, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(178)},
{TaskField.Deliverable Start, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(179)},
{TaskField.Deliverable Type, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(181)},
{TaskField.Earned Value Method, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(94)},
{TaskField.GUID, FieldLocation.FIXED_DATA, Integer.valueOf(0), Integer.valueOf(0)}, <--- not new, but in data2
{TaskField.Task Calendar GUID, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(182)},
////// Need to check these
{TaskField.Subproject File, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(160)},
{TaskField.Subproject Task ID, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(79)},
{TaskField.Subproject Tasks Unique ID Offset, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(8)},
{TaskField.Subproject Unique Task ID, FieldLocation.VAR_DATA, Integer.valueOf(65535), Integer.valueOf(9)},

   FIXED_DURATION(DataType.BOOLEAN),
   RESUME_NO_EARLIER_THAN(DataType.DATE),
   PARENT_TASK(DataType.INTEGER),
   INDEX(DataType.INTEGER),
   DURATION1_ESTIMATED(DataType.BOOLEAN),
   DURATION2_ESTIMATED(DataType.BOOLEAN),
   DURATION3_ESTIMATED(DataType.BOOLEAN),
   DURATION4_ESTIMATED(DataType.BOOLEAN),
   DURATION5_ESTIMATED(DataType.BOOLEAN),
   DURATION6_ESTIMATED(DataType.BOOLEAN),
   DURATION7_ESTIMATED(DataType.BOOLEAN),
   DURATION8_ESTIMATED(DataType.BOOLEAN),
   DURATION9_ESTIMATED(DataType.BOOLEAN),
   DURATION10_ESTIMATED(DataType.BOOLEAN),
   BASELINE_DURATION_ESTIMATED(DataType.BOOLEAN),
   HYPERLINK_SCREEN_TIP(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE1(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE2(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE3(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE4(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE5(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE6(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE7(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE8(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE9(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE10(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE11(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE12(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE13(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE14(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE15(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE16(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE17(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE18(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE19(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE20(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE21(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE22(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE23(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE24(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE25(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE26(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE27(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE28(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE29(DataType.STRING),
   ENTERPRISE_OUTLINE_CODE30(DataType.STRING),
   BASELINE1_DURATION_ESTIMATED(DataType.BOOLEAN),
   BASELINE2_DURATION_ESTIMATED(DataType.BOOLEAN),
   BASELINE3_DURATION_ESTIMATED(DataType.BOOLEAN),
   BASELINE4_DURATION_ESTIMATED(DataType.BOOLEAN),
   BASELINE5_DURATION_ESTIMATED(DataType.BOOLEAN),
   BASELINE6_DURATION_ESTIMATED(DataType.BOOLEAN),
   BASELINE7_DURATION_ESTIMATED(DataType.BOOLEAN),
   BASELINE8_DURATION_ESTIMATED(DataType.BOOLEAN),
   BASELINE9_DURATION_ESTIMATED(DataType.BOOLEAN),
   BASELINE10_DURATION_ESTIMATED(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_COST1(DataType.CURRENCY),
   ENTERPRISE_PROJECT_COST2(DataType.CURRENCY),
   ENTERPRISE_PROJECT_COST3(DataType.CURRENCY),
   ENTERPRISE_PROJECT_COST4(DataType.CURRENCY),
   ENTERPRISE_PROJECT_COST5(DataType.CURRENCY),
   ENTERPRISE_PROJECT_COST6(DataType.CURRENCY),
   ENTERPRISE_PROJECT_COST7(DataType.CURRENCY),
   ENTERPRISE_PROJECT_COST8(DataType.CURRENCY),
   ENTERPRISE_PROJECT_COST9(DataType.CURRENCY),
   ENTERPRISE_PROJECT_COST10(DataType.CURRENCY),
   ENTERPRISE_PROJECT_DATE1(DataType.DATE),
   ENTERPRISE_PROJECT_DATE2(DataType.DATE),
   ENTERPRISE_PROJECT_DATE3(DataType.DATE),
   ENTERPRISE_PROJECT_DATE4(DataType.DATE),
   ENTERPRISE_PROJECT_DATE5(DataType.DATE),
   ENTERPRISE_PROJECT_DATE6(DataType.DATE),
   ENTERPRISE_PROJECT_DATE7(DataType.DATE),
   ENTERPRISE_PROJECT_DATE8(DataType.DATE),
   ENTERPRISE_PROJECT_DATE9(DataType.DATE),
   ENTERPRISE_PROJECT_DATE10(DataType.DATE),
   ENTERPRISE_PROJECT_DATE11(DataType.DATE),
   ENTERPRISE_PROJECT_DATE12(DataType.DATE),
   ENTERPRISE_PROJECT_DATE13(DataType.DATE),
   ENTERPRISE_PROJECT_DATE14(DataType.DATE),
   ENTERPRISE_PROJECT_DATE15(DataType.DATE),
   ENTERPRISE_PROJECT_DATE16(DataType.DATE),
   ENTERPRISE_PROJECT_DATE17(DataType.DATE),
   ENTERPRISE_PROJECT_DATE18(DataType.DATE),
   ENTERPRISE_PROJECT_DATE19(DataType.DATE),
   ENTERPRISE_PROJECT_DATE20(DataType.DATE),
   ENTERPRISE_PROJECT_DATE21(DataType.DATE),
   ENTERPRISE_PROJECT_DATE22(DataType.DATE),
   ENTERPRISE_PROJECT_DATE23(DataType.DATE),
   ENTERPRISE_PROJECT_DATE24(DataType.DATE),
   ENTERPRISE_PROJECT_DATE25(DataType.DATE),
   ENTERPRISE_PROJECT_DATE26(DataType.DATE),
   ENTERPRISE_PROJECT_DATE27(DataType.DATE),
   ENTERPRISE_PROJECT_DATE28(DataType.DATE),
   ENTERPRISE_PROJECT_DATE29(DataType.DATE),
   ENTERPRISE_PROJECT_DATE30(DataType.DATE),
   ENTERPRISE_PROJECT_DURATION1(DataType.DURATION),
   ENTERPRISE_PROJECT_DURATION2(DataType.DURATION),
   ENTERPRISE_PROJECT_DURATION3(DataType.DURATION),
   ENTERPRISE_PROJECT_DURATION4(DataType.DURATION),
   ENTERPRISE_PROJECT_DURATION5(DataType.DURATION),
   ENTERPRISE_PROJECT_DURATION6(DataType.DURATION),
   ENTERPRISE_PROJECT_DURATION7(DataType.DURATION),
   ENTERPRISE_PROJECT_DURATION8(DataType.DURATION),
   ENTERPRISE_PROJECT_DURATION9(DataType.DURATION),
   ENTERPRISE_PROJECT_DURATION10(DataType.DURATION),
   ENTERPRISE_PROJECT_OUTLINE_CODE1(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE2(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE3(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE4(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE5(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE6(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE7(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE8(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE9(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE10(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE11(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE12(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE13(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE14(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE15(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE16(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE17(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE18(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE19(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE20(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE21(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE22(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE23(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE24(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE25(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE26(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE27(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE28(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE29(DataType.STRING),
   ENTERPRISE_PROJECT_OUTLINE_CODE30(DataType.STRING),
   ENTERPRISE_PROJECT_FLAG1(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG2(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG3(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG4(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG5(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG6(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG7(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG8(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG9(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG10(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG11(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG12(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG13(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG14(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG15(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG16(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG17(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG18(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG19(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_FLAG20(DataType.BOOLEAN),
   ENTERPRISE_PROJECT_NUMBER1(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER2(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER3(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER4(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER5(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER6(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER7(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER8(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER9(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER10(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER11(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER12(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER13(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER14(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER15(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER16(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER17(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER18(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER19(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER20(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER21(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER22(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER23(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER24(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER25(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER26(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER27(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER28(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER29(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER30(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER31(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER32(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER33(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER34(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER35(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER36(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER37(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER38(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER39(DataType.NUMERIC),
   ENTERPRISE_PROJECT_NUMBER40(DataType.NUMERIC),
   ENTERPRISE_PROJECT_TEXT1(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT2(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT3(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT4(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT5(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT6(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT7(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT8(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT9(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT10(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT11(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT12(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT13(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT14(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT15(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT16(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT17(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT18(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT19(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT20(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT21(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT22(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT23(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT24(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT25(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT26(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT27(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT28(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT29(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT30(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT31(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT32(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT33(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT34(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT35(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT36(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT37(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT38(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT39(DataType.STRING),
   ENTERPRISE_PROJECT_TEXT40(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE1(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE2(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE3(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE4(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE5(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE6(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE7(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE8(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE9(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE10(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE11(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE12(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE13(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE14(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE15(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE16(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE17(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE18(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE19(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE20(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE21(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE22(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE23(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE24(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE25(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE26(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE27(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE28(DataType.STRING),
   RESOURCE_ENTERPRISE_OUTLINE_CODE29(DataType.STRING),
   RESOURCE_ENTERPRISE_RBS(DataType.STRING),
   RESOURCE_ENTERPRISE_MULTI_VALUE_CODE20(DataType.STRING),
   RESOURCE_ENTERPRISE_MULTI_VALUE_CODE21(DataType.STRING),
   RESOURCE_ENTERPRISE_MULTI_VALUE_CODE22(DataType.STRING),
   RESOURCE_ENTERPRISE_MULTI_VALUE_CODE23(DataType.STRING),
   RESOURCE_ENTERPRISE_MULTI_VALUE_CODE24(DataType.STRING),
   RESOURCE_ENTERPRISE_MULTI_VALUE_CODE25(DataType.STRING),
   RESOURCE_ENTERPRISE_MULTI_VALUE_CODE26(DataType.STRING),
   RESOURCE_ENTERPRISE_MULTI_VALUE_CODE27(DataType.STRING),
   RESOURCE_ENTERPRISE_MULTI_VALUE_CODE28(DataType.STRING),
   RESOURCE_ENTERPRISE_MULTI_VALUE_CODE29(DataType.STRING),
   ACTUAL_WORK_PROTECTED(DataType.WORK),
   ACTUAL_OVERTIME_WORK_PROTECTED(DataType.WORK),
   BUDGET_WORK(DataType.WORK),
   BUDGET_COST(DataType.CURRENCY),
   RECALC_OUTLINE_CODES(DataType.BOOLEAN),

*/