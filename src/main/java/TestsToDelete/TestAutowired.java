package TestsToDelete;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

public class TestAutowired {
    public static void main(String[] args) {
        SomeClass someClass = new SomeClass();
        System.out.println(someClass.getAnotherClass().number);
    }
}
@Component
class SomeClass{
    @Autowired
    AnotherClass anotherClass;
//@Autowired
//    public SomeClass(AnotherClass anotherClass) {
//        this.anotherClass = anotherClass;
//    }

    public AnotherClass getAnotherClass() {
        return anotherClass;
    }
}
@Component
class AnotherClass{
    int number = 10;
}