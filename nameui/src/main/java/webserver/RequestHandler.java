package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;

import constants.HttpMethod;
import db.DataBase;
import lombok.extern.slf4j.Slf4j;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.GenerateHtmlUtils;
import util.HttpRequestUtils;
import util.HttpResponseUtils;

@Slf4j
public class RequestHandler extends Thread {
//    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;
    private static final String BASE_URL = "webapp";

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            // 헤더 읽어오기
            Map<String, String> headers = HttpRequestUtils.readHeaders(in);
            log.info("headers : \n{}", headers);

            // 헤더에서 첫 번째 라인 요청 URL 추출하기
            String url = headers.get("url");

            // POST, GET 구분
            if (HttpMethod.POST.getMethod().equals(headers.get("method"))) {
                if ("/user/login".equals(url)) {
                    Map<String, String> userString = HttpRequestUtils.parseQueryString(headers.get("content"));

                    User user = DataBase.findUserById(userString.get("userId"));

                    if (user != null && user.getPassword().equals(userString.get("password"))) { // 로그인 성공
                        byte[] body = Files.readAllBytes(new File(BASE_URL + "/index.html").toPath());

                        DataOutputStream dos = new DataOutputStream(out);
                        HttpResponseUtils.response302HeaderWithCookie(dos, body.length, "/index.html");
                        HttpResponseUtils.responseBody(dos, body);
                    }
                    else { // 로그인 실패
                        byte[] body = Files.readAllBytes(new File(BASE_URL + "/user/login_failed.html").toPath());

                        DataOutputStream dos = new DataOutputStream(out);
                        HttpResponseUtils.response401HeaderWithCookie(dos, body.length, "/user/login_failed.html");
                        HttpResponseUtils.responseBody(dos, body);
                    }
                }
                else if (url.equals("/user/create")) {
                    Map<String, String> userString = HttpRequestUtils.parseQueryString(headers.get("content"));

                    if (!userString.isEmpty()) {
                        User user = new User(userString.get("userId"), userString.get("password"), userString.get("name"), userString.get("email"));
                        DataBase.addUser(user);

                        byte[] body = Files.readAllBytes(new File(BASE_URL + "/index.html").toPath());

                        DataOutputStream dos = new DataOutputStream(out);
                        HttpResponseUtils.response302Header(dos, body.length, "/index.html");
                        HttpResponseUtils.responseBody(dos, body);
                    }
                }
            }
            else if(HttpMethod.GET.getMethod().equals(headers.get("method"))) {
                if("/user/list".equals(url)) {
                    Map<String, String> cookies = HttpRequestUtils.parseCookies(headers.get("Cookie"));
                    boolean isLogined = Boolean.parseBoolean(cookies.get("logined"));

                    if (isLogined) { // 로그인 상태
                        String body = new String(Files.readAllBytes(new File(BASE_URL + "/user/list.html").toPath()));
                        String userTableHtml = GenerateHtmlUtils.generateUserTableHtml(new ArrayList<>(DataBase.findAll()));

                        String resultStr = body.replace("                    {USER_TABLE}", userTableHtml);

                        // 응답 생성
                        byte[] result = resultStr.getBytes(StandardCharsets.UTF_8);
                        DataOutputStream dos = new DataOutputStream(out);
                        HttpResponseUtils.response200Header(dos, result.length);
                        HttpResponseUtils.responseBody(dos, result);

                    }
                    else { // 로그아웃 상태
                        byte[] body = Files.readAllBytes(new File(BASE_URL + "/user/login.html").toPath());

                        DataOutputStream dos = new DataOutputStream(out);
                        HttpResponseUtils.response401HeaderWithCookie(dos, body.length, "/user/login.html");
                        HttpResponseUtils.responseBody(dos, body);
                    }
                }
                else if(url.endsWith(".css")) {
                    byte[] css = Files.readAllBytes(new File(BASE_URL + url).toPath());

                    DataOutputStream dos = new DataOutputStream(out);
                    HttpResponseUtils.responseCss(dos, css.length);
                    HttpResponseUtils.responseBody(dos, css);
                }
                else {
                    // 요청 URL 에 해당하는 파일을 읽어서 전달
                    byte[] body = Files.readAllBytes(new File(BASE_URL + url).toPath());

                    DataOutputStream dos = new DataOutputStream(out);
                    HttpResponseUtils.response200Header(dos, body.length);
                    HttpResponseUtils.responseBody(dos, body);
                }
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }




}
