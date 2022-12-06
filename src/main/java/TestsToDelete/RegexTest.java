package TestsToDelete;

import java.util.HashMap;
import java.util.Map;

public class RegexTest {
    public static void main(String[] args) {
        Map<String,Integer> stringInteger = new HashMap<>();
        stringInteger.put("string",1);
        stringInteger.put("string",2);
        stringInteger.forEach((k,v)-> System.out.println(k + "-" + v));
        String regex = "https?://([\\w.]+/?)+";
        String text = "http://www.site.com/page/frhrhr//";
        System.out.println(text.matches(regex));
    }
}
