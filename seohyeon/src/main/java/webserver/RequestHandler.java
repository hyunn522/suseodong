package webserver;

import static util.HttpRequestHeaderUtils.*;
import static util.HttpRequestUtils.*;
import static util.IOUtils.*;

import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import java.nio.file.Files;
import java.util.Map;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = br.readLine();
            String requestMethod = parseRequestMethod(line);
            String requestUrl = parseRequestUrl(line);
            String requestHeader = "";
            String requestBody = "";

            log.info("----HTTP Request start----");
            log.info(line);
            while (!Strings.isNullOrEmpty(line)) {
                line = br.readLine();
                log.info(line);
                requestHeader += line + "\n";
            }

            Map<String, String> parsedHeader = parseRequestHeader(requestHeader);
            if (parsedHeader.containsKey("Content-Length")) {
                requestBody = readData(br, Integer.parseInt(parsedHeader.get("Content-Length")));
            }
            log.info("----HTTP Request end----");

            DataOutputStream dos = new DataOutputStream(out);
            byte[] body;
            if (!requestUrl.isEmpty()) {
                File file = new File("./webapp" + requestUrl);
                if (file.exists()) {
                    body = Files.readAllBytes(new File("./webapp" + requestUrl).toPath());
                } else {
                    if (requestUrl.startsWith("/user/create")) {
                        if (requestMethod.equals("GET")) {
                            int startPosition = requestUrl.indexOf("?");
                            String queryParams = requestUrl.substring(startPosition + 1);
                            Map<String, String> parsedQueryString = parseQueryString(queryParams);
                            signUp(parsedQueryString);
                        } else if (requestMethod.equals("POST")) {
                            Map<String, String> parsedRequestBody = parseQueryString(requestBody.toString());
                            signUp(parsedRequestBody);
                        }
                        log.info("SignUp with " + requestMethod);
                        body = "SignUp Success".getBytes();
                    } else {
                        body = "Invalid RequestUrl".getBytes();
                    }
                }
            } else {
                body = "Empty RequestUrl".getBytes();
            }

            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private User signUp(Map<String, String> parsedQueryString) {
        String userId = parsedQueryString.get("userId");
        String password = parsedQueryString.get("password");
        String name = parsedQueryString.get("name");
        String email = parsedQueryString.get("email");

        User user = new User(userId, password, name, email);
        log.info("user id: " + user.getUserId() + ", user password: " + user.getPassword() + ", user name: " + user.getName() + ", user email: " + user.getEmail());
        return user;
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
