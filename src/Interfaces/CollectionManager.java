package Interfaces;

import BasicClasses.Person;
import BasicClasses.StudyGroup;

import java.util.LinkedList;
import java.util.List;

public interface CollectionManager {
    void initList();

    LinkedList<StudyGroup> getLinkedList();

    void add(StudyGroup studyGroup);

    String getInfo();

    String show();

    void update(StudyGroup groupToUpdate, Integer elementId);

    void removeById(Integer groupId);

    void clear();

    String head();

    List<Integer> removeGreater(StudyGroup studyGroup, List<Integer> ids);

    List<Integer> removeLower(StudyGroup studyGroup, List<Integer> ids);

    String minBySemesterEnum();

    String maxByGroupAdmin();

    String countByGroupAdmin(Person groupAdmin);

    void appendToList(Object o);
}
