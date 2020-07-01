package Commands;

import BasicClasses.StudyGroup;

import java.io.Serializable;
import java.util.LinkedList;

public class SerializedCollection implements Serializable {
    private LinkedList<StudyGroup> linkedList;

    public SerializedCollection(LinkedList<StudyGroup> linkedList) {
        this.linkedList = linkedList;
    }

    public LinkedList<StudyGroup> getLinkedList() {
        return linkedList;
    }
}
