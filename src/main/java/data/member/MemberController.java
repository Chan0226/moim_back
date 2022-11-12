package data.member;

import data.config.BaseException;
import data.config.BaseResponse;
import data.config.BaseResponseStatus;
import data.config.JwtTokenUtil;
import data.member.model.*;
import data.seller.PostSellerReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigInteger;
import java.util.UUID;

import static data.config.BaseResponseStatus.*;
import static data.util.Validation.isValidatedIdx;

@CrossOrigin("http://localhost:3000")
@RestController
@RequestMapping("/member")
public class MemberController {
    private final MemberDao memberDao;

    @Autowired
    MemberService memberService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private EmailCertService emailCertService;

    @Autowired
    public MemberController(MemberDao memberDao) {
        this.memberDao = memberDao;
    }


    @ResponseBody
    @PostMapping("/signup")
    public BaseResponse<PostMemberRes> createMember(@RequestBody PostMemberReq postMemberReq) {

        try {
            if (postMemberReq.getNickname() == null) {
                return new BaseResponse<>(POST_USER_NICKNAME_NULL);
            }

            String token = UUID.randomUUID().toString();
            System.out.println("====== Controller : createMember : Req ====== " + postMemberReq);

            PostMemberRes postMemberRes = memberService.createMember(postMemberReq, token);
            UserDetails userDetails = memberService.findByEmailStatusZero(postMemberReq.getEmail());

            final String jwt = jwtTokenUtil.generateToken(userDetails);

            emailCertService.createEmailConfirmationToken(token, postMemberReq.getEmail(), jwt);

            return new BaseResponse<>(postMemberRes);

        } catch (Exception exception) {
            System.out.println(exception);
            return new BaseResponse<>(BaseResponseStatus.FAIL);
        }
    }

    @ResponseBody
    @GetMapping("/confirm")
    public RedirectView signupConfirm(GetEmailConfirmReq getEmailConfirmReq) throws Exception {
        GetEmailCertRes getEmailCertRes = emailCertService.signupConfirm(getEmailConfirmReq);
        return new RedirectView("http://localhost:3000/emailconfirm/" + getEmailConfirmReq.getJwt());
    }

    @ResponseBody
    @GetMapping("/modify")
    public BaseResponse<GetMemberRes> getModifyMemberInfo(@AuthenticationPrincipal UserLoginRes userLoginRes) {

        if (userLoginRes == null) {
            return new BaseResponse<>(NOT_LOGIN);
        }
        try {
            BigInteger userIdx = userLoginRes.getIdx();
            GetMemberRes getMemberRes = memberService.getModifyMemberInfo(userIdx);
            return new BaseResponse<>(getMemberRes);

        } catch (Exception exception) {
            return new BaseResponse<>(EMPTY_IDX);
        }
    }


    @ResponseBody
    @PatchMapping("/modify/{idx}")
    public BaseResponse<String> modifyMemberInfo(@AuthenticationPrincipal UserLoginRes userLoginRes, @PathVariable("idx") BigInteger idx, @RequestBody PatchMemberModityReq patchMemberModityReq) {

        if (idx == null) {
            return new BaseResponse<>(EMPTY_IDX);
        }
        if (!isValidatedIdx(idx)) {
            return new BaseResponse<>(INVALID_IDX);
        }
        if (userLoginRes == null) {
            return new BaseResponse<>(NOT_LOGIN);
        }

        try {
            BigInteger userIdx = userLoginRes.getIdx();
            System.out.println("== userLoginRes.getIdx: " + userLoginRes.getIdx() + ", Idx: " + idx);

            if (!userIdx.equals(idx)) {
                return new BaseResponse<>(INVALID_USER_JWT);
            }

            memberService.modifyMemberInfo(patchMemberModityReq, idx);
            String result = patchMemberModityReq.getNickname() + "로 변경 완료.";
            return new BaseResponse<>(result);
        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

    @ResponseBody
    @PostMapping("/sellersignup")
    public BaseResponse<PostMemberRes> createSeller(@RequestBody PostSellerReq postSellerReq) {

        try {
            System.out.println("========================== Req: " + postSellerReq);

            PostMemberRes postMemberRes = memberService.createSeller(postSellerReq);
            return new BaseResponse<>(postMemberRes);

        } catch (Exception exception) {
            System.out.println(exception);
            return new BaseResponse<>(BaseResponseStatus.FAIL);
        }
    }


    //     계정 탈퇴 API
    @ResponseBody
    @PatchMapping("/delete/{idx}")
    public BaseResponse<GetMemberRes> deleteUser(@PathVariable BigInteger idx) {
        if (idx == null) {
            return new BaseResponse<>(EMPTY_IDX);
        }
        if (!isValidatedIdx(idx)) {
            return new BaseResponse<>(INVALID_IDX);
        }

        try {
            GetMemberRes getMemberRes = memberService.deleteUser(idx);
            return new BaseResponse<>(getMemberRes);
        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

    @ResponseBody
    @GetMapping("{email}")
    public Boolean getUserEmail(@PathVariable("email") String email) {
        Boolean result = memberService.getUserEmail(email);
        return result;
    }

    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) throws Exception {

        if (authenticationRequest.getUsername().length() == 0) {
            System.out.println("username is NULL");
        }

        if (authenticationRequest.getPassword().length() == 0) {
            System.out.println("Password is NULL");
        }

// 탈퇴한 회원인지 확인.
//        if (!memberDao.isValidStatus(authenticationRequest)) {
//            System.out.println("탈퇴한 회원");
//        }

        System.out.println(authenticationRequest.getUsername() + ", " + authenticationRequest.getPassword());

        Authentication authentication = authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        UserLoginRes userLoginRes = (UserLoginRes) authentication.getPrincipal();

        final String token = jwtTokenUtil.generateToken(userLoginRes);

        return ResponseEntity.ok(new JwtResponse(token));
    }

    private Authentication authenticate(String username, String password) throws Exception {

        try {
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new DisabledException("USER_DISABLED", e);
        } catch (AccountExpiredException e) {
            throw new AccountExpiredException("AccountExpiredException", e);
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("비민번호 오류 입니다. INVALID_CREDENTIALS", e);
        } catch (InternalAuthenticationServiceException e) {
            throw new InternalAuthenticationServiceException("존재하지 않는 아이디 입니다. InternalAuthenticationServiceException", e);
        }
        catch (AuthenticationCredentialsNotFoundException e) {
            throw new AuthenticationCredentialsNotFoundException("인증 요구 거부.", e);
        }
    }
}
