package data.member;

import data.member.model.*;
import data.seller.PostSellerReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;


@Repository
public class MemberDao {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    public PostMemberRes createMember(PostMemberReq postMemberReq) {

        String createMemberQuery = "insert into member (email, password, nickname, phoneNum, gender, birthday, notification) VALUES (?, ?, ?, ?, ?, ?, ?)";

        Object[] createMemberParams = new Object[]{postMemberReq.getEmail(), postMemberReq.getPassword(), postMemberReq.getNickname(), postMemberReq.getPhoneNum(), postMemberReq.getGender(), postMemberReq.getBirthday(), postMemberReq.getNotification()
        };

        this.jdbcTemplate.update(createMemberQuery, createMemberParams);

        String getLastInsertIdxQuery = "select last_insert_id()";
        int lastInsertIdx = this.jdbcTemplate.queryForObject(getLastInsertIdxQuery, int.class);

        String createAuthorityQuery = "insert into authority values (?, ?)";

        Object[] createAuthorityParams = new Object[]{lastInsertIdx, 0};

        this.jdbcTemplate.update(createAuthorityQuery, createAuthorityParams);

        return new PostMemberRes(lastInsertIdx, 1);
    }

    public int updatePassword(String password, String email) {
        System.out.println(password);
        String modifyMemberQuery = "update member set password=? where email=?";
        Object[] modifyMemberParams = new Object[]{password, email};

        return this.jdbcTemplate.update(modifyMemberQuery, modifyMemberParams);
    }


    public Integer createMemberKakao(String kakaoemail, String nickname, String profile_image) {

        String createMemberQuery = "insert into member (email, password, nickname, profile_image) VALUES (?, ?, ?, ?)";

        Object[] createMemberParams = new Object[]{kakaoemail, "kakao", nickname, profile_image
        };

        this.jdbcTemplate.update(createMemberQuery, createMemberParams);

        String getLastInsertIdxQuery = "select last_insert_id()";
        int lastInsertIdx = this.jdbcTemplate.queryForObject(getLastInsertIdxQuery, int.class);

        String createAuthorityQuery = "insert into authority values(?, ?)";

        Object[] createAuthorityParams = new Object[]{lastInsertIdx, 0};

        this.jdbcTemplate.update(createAuthorityQuery, createAuthorityParams);

        return lastInsertIdx;
    }


    public PostMemberRes createSeller(PostSellerReq postSellerReq) {

        String createMemberQuery = "insert into host (email, password, companyName, businessNumber, logoImage, phone, address, bank, accountNumber ) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Object[] createMemberParams = new Object[]{postSellerReq.getEmail(), postSellerReq.getPassword(), postSellerReq.getCompanyName(), postSellerReq.getBusinessNumber(), postSellerReq.getLogoImage(), postSellerReq.getPhone(), postSellerReq.getAddress(),
                postSellerReq.getBank(),postSellerReq.getAccountNumber()
        };

        this.jdbcTemplate.update(createMemberQuery, createMemberParams);

        String getLastInsertIdxQuery = "select last_insert_id()";
        int lastInsertIdx = this.jdbcTemplate.queryForObject(getLastInsertIdxQuery, int.class);

        return new PostMemberRes(lastInsertIdx, 1);
    }

    public UserLoginRes findByEmailStatusZero(String email) {
        String getEmailQuery = "SELECT * FROM member LEFT OUTER JOIN authority on member.idx=authority.member_idx WHERE email=? AND status=0";

        return this.jdbcTemplate.queryForObject(getEmailQuery
                , (rs, rowNum) -> new UserLoginRes(
                        rs.getObject("idx", BigInteger.class),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("nickname"),
                        rs.getString("profile_image"),
                        Arrays.asList(new SimpleGrantedAuthority(Authority.values()[rs.getObject("role", int.class)].toString()))
                ), email);
    }

    public boolean isValidStatus(JwtRequest authenticationRequest) {
        String checkStatusQuery = "select status from member where email = ?";
        String checkStatusParams = authenticationRequest.getUsername();

        Integer status = this.jdbcTemplate.queryForObject(checkStatusQuery
                , Integer.class
                , checkStatusParams);

        return (status == 1);
    }

    public boolean isValidSellerStatus(JwtRequest authenticationRequest) {
        String checkStatusQuery = "select active from host where email = ?";
        String checkStatusParams = authenticationRequest.getUsername();

        Integer status = this.jdbcTemplate.queryForObject(checkStatusQuery
                , Integer.class
                , checkStatusParams);

        return (status == 1);
    }


    public UserLoginRes findByEmail(String email) {
        String getEmailQuery = "SELECT * FROM member LEFT OUTER JOIN authority on member.idx=authority.member_idx WHERE email=? ";//AND status=1";

        return this.jdbcTemplate.queryForObject(getEmailQuery
                , (rs, rowNum) -> new UserLoginRes(
                        rs.getObject("idx", BigInteger.class),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("nickname"),
                        rs.getString("profile_image"),
                        Arrays.asList(new SimpleGrantedAuthority(Authority.values()[rs.getObject("role", int.class)].toString()))
                ), email);
    }

    public Boolean getUserEmail(String email) {
        String findEmailQuery = "SELECT * FROM member WHERE email=?";
        try {
            UserLoginRes userLoginRes = this.jdbcTemplate.queryForObject(findEmailQuery
                    , (rs, rowNum) -> new UserLoginRes(
                            rs.getObject("idx", BigInteger.class),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("nickname"),
                            rs.getString("profile_image"),
                            new ArrayList<>()
                    ), email);
            if (userLoginRes.getEmail() != null) {
                return true;

            } else {
                return false;
            }

        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    public int checkEmail(String email) {
        String checkEmailQuery = "select exists(select email from member where email = ?)";
        String checkEmailParams = email;
        return this.jdbcTemplate.queryForObject(checkEmailQuery,
                int.class,
                checkEmailParams);

    }

    public MemberInfo getUserGradeAndPoint(BigInteger idx) {
        String findEmailQuery = "SELECT * FROM member WHERE idx=?";
        return this.jdbcTemplate.queryForObject(findEmailQuery
                , (rs, rowNum) -> new MemberInfo(
                        rs.getString("email"),
                        rs.getString("nickname"),
                        rs.getObject("point", int.class),
                        rs.getString("grade")
                ), idx);
    }

    public void setMemberPoint(BigInteger idx, int point) {
        String plusUserPointQuery = "update member set point = ?  where idx = ?";
        Object[] plusUserPointParams = new Object[]{point, idx};
        this.jdbcTemplate.update(plusUserPointQuery, plusUserPointParams);
    }

    public GetMemberRes getModifyMemberInfo(BigInteger userIdx) {

        String modifyMemberQuery = "select email,nickname, phoneNum, gender, birthday, notification from member where idx=?";

        return this.jdbcTemplate.queryForObject(modifyMemberQuery
                , (rs, rowNum) -> new GetMemberRes(
                        rs.getString("email"),
                        rs.getString("nickname"),
                        rs.getString("phoneNum"),
                        rs.getString("gender"),
                        rs.getString("birthday"),
                        rs.getString("notification")), userIdx);
    }

    public int modifyMemberInfo(PatchMemberModityReq patchMemberModityReq, BigInteger idx) {
        System.out.println(patchMemberModityReq.toString());
        String modifyMemberQuery = "update member set nickname=?, phoneNum=?, gender=?, birthday=?, notification=? where idx=?";
        Object[] modifyMemberParams = new Object[]{patchMemberModityReq.getNickname(), patchMemberModityReq.getPhoneNum(), patchMemberModityReq.getGender(), patchMemberModityReq.getBirthday(), patchMemberModityReq.getNotification(), idx
        };

        return this.jdbcTemplate.update(modifyMemberQuery, modifyMemberParams);
    }

    //    삭제
    public void deleteUser(BigInteger idx) {
        String deleteUserQuery = "delete from member where idx = ?";
        Object[] deleteUserParams = new Object[]{idx};

        this.jdbcTemplate.update(deleteUserQuery, deleteUserParams);
    }

    public GetMemberRes getDeletedUser(BigInteger idx) {
        String getDeletedUserQuery = "select * from member where idx = ? and status = 0";
        BigInteger getDeletedUserParams = idx;
        return this.jdbcTemplate.queryForObject(getDeletedUserQuery,
                (rs, rowNum) -> new GetMemberRes(
                        rs.getString("email"),
                        rs.getString("nickname"),
                        rs.getString("phoneNum"),
                        rs.getString("gender"),
                        rs.getString("birthday"),
                        rs.getString("notification")),
                getDeletedUserParams);
    }

    public boolean isNotExistedUser(BigInteger idx) {
        String checkNullQuery = "select count(case when idx = ? and status = 1 then 1 end) from member";
        Integer isExistedNum = this.jdbcTemplate.queryForObject(checkNullQuery, Integer.class, idx);
        return (isExistedNum.equals(0));
    }

    public boolean isDeletedUser(BigInteger idx) {
        String checkDeletedUserQuery = "select status from member where idx = ?";
        BigInteger checkDeletedUserParams = idx;

        Integer status = this.jdbcTemplate.queryForObject(checkDeletedUserQuery
                , Integer.class
                , checkDeletedUserParams);

        return (status == 0);
    }

}

