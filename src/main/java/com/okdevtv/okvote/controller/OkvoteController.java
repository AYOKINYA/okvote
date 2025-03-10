package com.okdevtv.okvote.controller;

import com.okdevtv.okvote.model.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@Validated
public class OkvoteController {
  @Autowired
  private QuestionRepository repository;
  @Autowired
  private AnswerRepository answerRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private VoteRepository voteRepository;
  @Autowired
  private AnswerResultRepository answerResultRepository;

  @GetMapping("/{qno}")
  public String index(@PathVariable(name = "qno", required = false) Long qno, Model model) {
    Question question = repository.findById(qno).get();
    model.addAttribute("question", question.getQuestion());
    List<Answer> answers = answerRepository.findByQuestionId(qno);
    model.addAttribute("answers", answers);

    return "index";
  }

  @PostMapping("/{qno}")
  public Object index(@PathVariable("qno") Long qno,
                      @CookieValue(name = "name", required = false) String name,
                      @RequestParam("selected") Long answerId) {
    if (name == null) {
      return new RedirectView("/login");
    }
    User user = userRepository.findUserByName(name);
    Long userId = user.getId();
    voteRepository.save(new Vote(userId, answerId));
    return new RedirectView("/result/" + qno);
  }

  @RequestMapping("/form")
  public Object form(Model model, @CookieValue(name = "name", required = false) String name) {
    if (name == null) {
      return new RedirectView("/login");
    }
    model.addAttribute("name", name);
    return "form";
  }

  @PostMapping("/form")
  public RedirectView form(@RequestParam @NotBlank String question,
                           @RequestParam(name = "answer") @NotEmpty List<@NotBlank String> answers,
                           @CookieValue(name = "name", required = false) String name) {
    User user = userRepository.findUserByName(name);

    Question question1 = repository.save(new Question(user.getId(), question));
    for (String answer : answers) {
      answerRepository.save(new Answer(question1.getId(), answer));
    }
    return new RedirectView("/" + question1.getId());
  }

  @RequestMapping("/login")
  public String login() {
    return "login";
  }

  @PostMapping("/login")
  public RedirectView loginProcess(HttpServletRequest request, HttpServletResponse response, @RequestParam String name) {
    String lowerName = name.toLowerCase();
    Cookie cookie = new Cookie("name", lowerName);
    cookie.setMaxAge(60 * 60 * 24);
    cookie.setPath("/");
    response.addCookie(cookie);
    HttpSession session = request.getSession();
    session.setAttribute("name", lowerName);
    User userByName = userRepository.findUserByName(lowerName);
    if (userByName == null) {
      userRepository.save(new User(lowerName));
    }
    return new RedirectView("/form");
  }

  @GetMapping("/logout")
  public RedirectView logout(HttpServletRequest request, HttpServletResponse response) {
    Cookie cookie = new Cookie("name", null);
    cookie.setMaxAge(0);
    cookie.setPath("/");
    response.addCookie(cookie);
    request.getSession().removeAttribute("name");
    return new RedirectView("/");
  }

  @RequestMapping("/result/{qno}")
  public String result(@PathVariable("qno") Long qno, Model model) {
    Question question = repository.findById(qno).get();
    List<AnswerDto> answers = answerResultRepository.findAnswerResult(qno);
    Long total = 0L;
    for(AnswerDto answer : answers) {
      total += answer.getCnt();
    }
    Long finalTotal = total;
    answers.forEach(answer -> {
      Long percent = answer.getCnt() * 100 / finalTotal;
      answer.setPercent(percent);
    });

    model.addAttribute("question", question);
    model.addAttribute("answers", answers);
    return "result";
  }

  @RequestMapping("/list")
  public String list(Model model) {
    Sort sort = Sort.by(Sort.Direction.DESC, "id");
    Iterable<Question> questions = repository.findAll(sort);
    model.addAttribute("questions", questions);
    return "list";
  }
}
