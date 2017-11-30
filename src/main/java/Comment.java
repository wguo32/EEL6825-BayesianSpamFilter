public class Comment {

    String comment_id;
    String author;
    String date;
    String content;
    Boolean spam;
    double spamicity;

    public Comment(String comment_id, String author, String date, String content, Boolean spam) {
        this.comment_id = comment_id;
        this.author = author;
        this.date = date;
        this.content = content;
        this.spam = spam;
    }


}
