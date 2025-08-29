import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class SocialNetwork {
    
    static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        String username;
        String password;
        String name;
        String bio;
        Set<String> following;
        List<Post> posts;
        Map<String, List<Message>> inbox;

        public User(String username, String password) {
            this.username = username;
            this.password = password;
            this.name = username;
            this.bio = "";
            this.following = new HashSet<>();
            this.posts = new ArrayList<>();
            this.inbox = new HashMap<>();
        }

        public void receiveMessage(Message m) {
            inbox.computeIfAbsent(m.from, k -> new ArrayList<>()).add(m);
        }

        @Override
        public String toString() {
            return String.format("%s (%s)\nBio: %s\nFollowers: %d  Following: %d  Posts: %d",
                    name, username, bio, SocialNetwork.data.countFollowers(username),
                    following.size(), posts.size());
        }
    }

    static class Post implements Serializable {
        private static final long serialVersionUID = 1L;
        static long nextId = 1;
        long id;
        String author;
        String text;
        Date createdAt;
        Set<String> likes;
        List<Comment> comments;

        public Post(String author, String text) {
            this.id = nextId++;
            this.author = author;
            this.text = text;
            this.createdAt = new Date();
            this.likes = new HashSet<>();
            this.comments = new ArrayList<>();
        }

        public String shortInfo() {
            return String.format("Post#%d by %s - %s", id, author, relativeTime(createdAt));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("----------\n");
            sb.append(String.format("Post #%d by %s at %s\n", id, author,
                    new SimpleDateFormat("yyyy-MM-dd HH:mm").format(createdAt)));
            sb.append(text).append("\n");
            sb.append(String.format("Likes: %d  Comments: %d\n", likes.size(), comments.size()));
            if (!comments.isEmpty()) {
                sb.append("Comments:\n");
                for (Comment c : comments) sb.append(" - ").append(c).append("\n");
            }
            sb.append("----------");
            return sb.toString();
        }

        static String relativeTime(Date d) {
            long diff = (new Date().getTime() - d.getTime()) / 1000;
            if (diff < 60) return diff + "s ago";
            if (diff < 3600) return (diff / 60) + "m ago";
            if (diff < 86400) return (diff / 3600) + "h ago";
            return (diff / 86400) + "d ago";
        }
    }

    static class Comment implements Serializable {
        private static final long serialVersionUID = 1L;
        String from;
        String text;
        Date at;

        public Comment(String from, String text) {
            this.from = from;
            this.text = text;
            this.at = new Date();
        }

        @Override
        public String toString() {
            return String.format("%s: %s (%s)", from, text,
                    new SimpleDateFormat("yyyy-MM-dd HH:mm").format(at));
        }
    }

    static class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        String from;
        String to;
        String text;
        Date at;

        public Message(String from, String to, String text) {
            this.from = from;
            this.to = to;
            this.text = text;
            this.at = new Date();
        }

        @Override
        public String toString() {
            return String.format("[%s] %s -> %s: %s",
                    new SimpleDateFormat("yyyy-MM-dd HH:mm").format(at),
                    from, to, text);
        }
    }

    static class SocialData implements Serializable {
        private static final long serialVersionUID = 1L;
        Map<String, User> users;
        Map<Long, Post> allPosts;
        long postIdCounter;

        public SocialData() {
            users = new HashMap<>();
            allPosts = new HashMap<>();
            postIdCounter = Post.nextId;
        }

        public int countFollowers(String username) {
            int count = 0;
            for (User u : users.values()) {
                if (u.following.contains(username)) count++;
            }
            return count;
        }
    }

    static SocialData data = new SocialData();
    static final String SAVE_FILE = "social_data.ser";
    static Scanner sc = new Scanner(System.in);

    static void pause() {
        System.out.println("Press Enter to continue...");
        sc.nextLine();
    }

    static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    static void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            data.postIdCounter = Post.nextId;
            oos.writeObject(data);
        } catch (IOException e) {
            System.out.println("Failed to save data: " + e.getMessage());
        }
    }

    static void loadData() {
        File f = new File(SAVE_FILE);
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            data = (SocialData) ois.readObject();
            Post.nextId = Math.max(1, data.postIdCounter);
        } catch (Exception e) {
            System.out.println("Failed to load data: " + e.getMessage());
        }
    }

    static User registerUser() {
        String username = readLine("Choose username: ");
        if (data.users.containsKey(username)) {
            System.out.println("Username already taken!");
            return null;
        }
        String password = readLine("Choose password: ");
        User u = new User(username, password);
        data.users.put(username, u);
        saveData();
        System.out.println("Registered successfully!");
        return u;
    }

    static User login() {
        String username = readLine("Username: ");
        String password = readLine("Password: ");
        User u = data.users.get(username);
        if (u != null && u.password.equals(password)) {
            System.out.println("Login successful!");
            return u;
        }
        System.out.println("Invalid credentials.");
        return null;
    }

    static void editProfile(User u) {
        u.name = readLine("Enter display name: ");
        u.bio = readLine("Enter bio: ");
        saveData();
        System.out.println("Profile updated.");
    }

    static void createPost(User u) {
        String text = readLine("Write your post: ");
        Post p = new Post(u.username, text);
        u.posts.add(p);
        data.allPosts.put(p.id, p);
        saveData();
        System.out.println("Post created!");
    }

    static void viewFeed(User u) {
        List<Post> feed = new ArrayList<>();
        for (String f : u.following) {
            User other = data.users.get(f);
            if (other != null) feed.addAll(other.posts);
        }
        feed.addAll(u.posts);
        feed.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
        if (feed.isEmpty()) {
            System.out.println("Feed is empty.");
        } else {
            for (Post p : feed) {
                System.out.println(p);
            }
        }
    }

    static void followUser(User u) {
        String target = readLine("Enter username to follow: ");
        if (!data.users.containsKey(target)) {
            System.out.println("User not found.");
            return;
        }
        if (target.equals(u.username)) {
            System.out.println("You cannot follow yourself.");
            return;
        }
        u.following.add(target);
        saveData();
        System.out.println("Now following " + target);
    }

    static void unfollowUser(User u) {
        String target = readLine("Enter username to unfollow: ");
        if (u.following.remove(target)) {
            saveData();
            System.out.println("Unfollowed " + target);
        } else {
            System.out.println("You were not following " + target);
        }
    }

    static void viewProfile() {
        String target = readLine("Enter username: ");
        User other = data.users.get(target);
        if (other == null) {
            System.out.println("User not found.");
            return;
        }
        System.out.println(other);
    }

    static void likePost(User u) {
        long id = Long.parseLong(readLine("Enter post ID: "));
        Post p = data.allPosts.get(id);
        if (p == null) {
            System.out.println("Post not found.");
            return;
        }
        p.likes.add(u.username);
        saveData();
        System.out.println("Liked post " + id);
    }

    static void commentPost(User u) {
        long id = Long.parseLong(readLine("Enter post ID: "));
        Post p = data.allPosts.get(id);
        if (p == null) {
            System.out.println("Post not found.");
            return;
        }
        String text = readLine("Enter comment: ");
        p.comments.add(new Comment(u.username, text));
        saveData();
        System.out.println("Comment added.");
    }

    static void sendMessage(User u) {
        String to = readLine("Send to: ");
        User other = data.users.get(to);
        if (other == null) {
            System.out.println("User not found.");
            return;
        }
        String text = readLine("Message: ");
        Message m = new Message(u.username, to, text);
        other.receiveMessage(m);
        saveData();
        System.out.println("Message sent.");
    }

    static void viewInbox(User u) {
        if (u.inbox.isEmpty()) {
            System.out.println("Inbox empty.");
            return;
        }
        for (List<Message> msgs : u.inbox.values()) {
            for (Message m : msgs) {
                System.out.println(m);
            }
        }
    }

    static void searchUsers() {
        String term = readLine("Search term: ");
        for (User u : data.users.values()) {
            if (u.username.contains(term) || u.name.contains(term)) {
                System.out.println(u.username + " (" + u.name + ")");
            }
        }
    }

    static void userMenu(User u) {
        while (true) {
            System.out.println("\n--- User Menu (" + u.username + ") ---");
            System.out.println("1. Edit Profile");
            System.out.println("2. Create Post");
            System.out.println("3. View Feed");
            System.out.println("4. Follow User");
            System.out.println("5. Unfollow User");
            System.out.println("6. View Profile");
            System.out.println("7. Like a Post");
            System.out.println("8. Comment on a Post");
            System.out.println("9. Send Message");
            System.out.println("10. View Inbox");
            System.out.println("11. Search Users");
            System.out.println("0. Logout");
            String ch = readLine("Choice: ");
            switch (ch) {
                case "1": editProfile(u); break;
                case "2": createPost(u); break;
                case "3": viewFeed(u); break;
                case "4": followUser(u); break;
                case "5": unfollowUser(u); break;
                case "6": viewProfile(); break;
                case "7": likePost(u); break;
                case "8": commentPost(u); break;
                case "9": sendMessage(u); break;
                case "10": viewInbox(u); break;
                case "11": searchUsers(); break;
                case "0": return;
                default: System.out.println("Invalid choice.");
            }
            pause();
        }
    }

    static void mainMenu() {
        while (true) {
            System.out.println("\n--- Social Network ---");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("0. Exit");
            String ch = readLine("Choice: ");
            switch (ch) {
                case "1":
                    User r = registerUser();
                    if (r != null) userMenu(r);
                    break;
                case "2":
                    User l = login();
                    if (l != null) userMenu(l);
                    break;
                case "0":
                    saveData();
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

 public static void main(String[] args) {
        loadData();
        if (data.users.isEmpty()) {
            User alice = new User("alice", "pass");
            alice.name = "Alice Example";
            alice.bio = "Hello! I'm Alice.";
            data.users.put(alice.username, alice);

            User bob = new User("bob", "123");
            bob.name = "Bob Example";
            bob.bio = "Bob here :)";
            data.users.put(bob.username, bob);

            Post p1 = new Post("alice", "Welcome to the demo social app!");
            alice.posts.add(p1);
            data.allPosts.put(p1.id, p1);

            Post p2 = new Post("bob", "This is Bob's first post.");
            bob.posts.add(p2);
            data.allPosts.put(p2.id, p2);

            saveData();
            System.out.println("Demo users created: alice/pass, bob/123");
        }
        mainMenu();
    }
}
