package Interfaces;

import BasicClasses.Person;
import BasicClasses.StudyGroup;

public interface Validator {
    boolean checkExistColor(String toContains);

    boolean checkExistCountry(String toContains);

    boolean checkExistFormOfEducation(String toContains);

    boolean checkExistSemester(String toContains);

    boolean validateStudyGroup(StudyGroup studyGroup);

    boolean validatePerson(Person person);
}
