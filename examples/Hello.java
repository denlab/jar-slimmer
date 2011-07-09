public class Hello {
    public static void main(String[] args) throws Exception {
        SayHello hello = (SayHello) Class.forName("SayHello").newInstance();
        System.out.println(hello.say());
    }
}

/* Used but not statically referenced by the entry point */

class SayHello {
    public String say() {
        return "Hello";
    }
}

/* The classes below are not used */

class NotNeeded1 {
    public String say() {
        return "Not needed 1";
    }
}

class NotNeeded2 {
    public String say() {
        return "Not needed 2";
    }
}

class NotNeeded3 {
    public String say() {
        return "Not needed 3";
    }
}

