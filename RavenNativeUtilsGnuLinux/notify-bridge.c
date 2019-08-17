
#include <libnotify/notify.h>
#include <gdk-pixbuf/gdk-pixbuf.h>
#include <gmodule.h>
#include <gio/gio.h>
char* read_line_from_std() {
char *line = NULL;
size_t size;
if (getline(&line, &size, stdin) == -1) {
   return "";
} else {
    line[strlen(line) - 1] = '\0';
    return line;
}
}
int main(int argv, char** argc) {
    int at_point = 0;
    char* title = read_line_from_std();
    char* content = read_line_from_std();
    if(strcmp(title, "") == 0 || strcmp(content, "") == 0) {
        return 0;
    }
    char* image_path = read_line_from_std();
     notify_init("Raven");
     NotifyNotification* n = notify_notification_new (title,
                                 content,
                                  0);
    if(strcmp(image_path, "") != 0) {
    GError *error = NULL;
    GError *error2 = NULL;
    GFile * fileRef = g_file_new_for_uri(image_path);
    GFileInputStream *stream = g_file_read(fileRef, NULL, NULL);
    GDataInputStream * data_stream = g_data_input_stream_new(stream);
    GdkPixbuf* buff = gdk_pixbuf_new_from_stream(data_stream, NULL, &error);
      notify_notification_set_image_from_pixbuf(n, buff); 
    }
    if (!notify_notification_show(n, 0)) 
    {
        return -1;
    }
    return 0;
}