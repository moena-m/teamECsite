package jp.co.internous.node.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import jp.co.internous.node.model.domain.MstUser;
import jp.co.internous.node.model.form.UserForm;
import jp.co.internous.node.model.mapper.MstUserMapper;
import jp.co.internous.node.model.mapper.TblCartMapper;
import jp.co.internous.node.model.session.LoginSession;

@RestController
@RequestMapping("/node/auth") 
public class AuthController {
	
	private Gson gson = new Gson();
	
	@Autowired
	private LoginSession loginSession;

	@Autowired
	private  MstUserMapper userMapper;

	@Autowired
	private TblCartMapper cartMapper;
	

		//	login機能
	    @RequestMapping("/login")
		public String login(@RequestBody UserForm form) {
			MstUser user = userMapper.findByUserNameAndPassword(form.getUserName(), form.getPassword());
			long tmpUserId = loginSession.getTmpUserId();
			
			// 仮IDでカートに商品が追加されていれば、本ユーザーIDに更新。
			if (user != null && tmpUserId != 0) {
				long count = cartMapper.findCountByUserId(tmpUserId);
				if (count > 0) {
					cartMapper.updateUserId(user.getId(), tmpUserId);
				}
			}
			//ログインに成功したらセッションの値更新
			if (user != null) {
				loginSession.setTmpUserId(0);
				loginSession.setLogined(true);
				loginSession.setUserId(user.getId());
				loginSession.setUserName(user.getUserName());
				loginSession.setPassword(user.getPassword());
			} else {//ログイン失敗
				loginSession.setLogined(false);
				loginSession.setUserId(0);
				loginSession.setUserName(null);
				loginSession.setPassword(null);
			}
			return gson.toJson(user);
		}
	

	    //ログアウト機能 セッションの更新
		@RequestMapping("/logout")
		public String logout() {
			loginSession.setTmpUserId(0);
			loginSession.setLogined(false);
			loginSession.setUserId(0);
			loginSession.setUserName(null);
			loginSession.setPassword(null);
			//トップページへ遷移
			return "";
		}

		@RequestMapping("/resetPassword")
		//リセットパスワード
		public String resetPassword(@RequestBody UserForm form){
			String message = "パスワードが再設定されました。";
			String newPassword = form.getNewPassword();
			String newPasswordConfirm = form.getNewPasswordConfirm();
			
			//MstserMapperからメソッドを呼び、formに入っているNameとpasswordでユーザーの存在を検索
			MstUser user = userMapper.findByUserNameAndPassword(form.getUserName(), form.getPassword());
			//認証失敗した場合
			if (user== null) {
				return "現在のパスワードが正しくありません。";
			 //現パスワードと新しいパスワードが一致する場合
			} else if (user.getPassword().equals(newPassword)) {
				return "現在のパスワードと同一文字列が入力されました。";
			 //新しいパスワードと確認用パスワード確認が一致しない場合
			} else if (!newPassword.equals(newPasswordConfirm)) {
				return "新パスワードと確認用パスワードが一致しません。";
			} else {
			    // mst_userとloginSessionのパスワードを更新する
			    userMapper.updatePassword(user.getUserName(), form.getNewPassword());
			    loginSession.setPassword(form.getNewPassword());
			}
			return message;
		}
}
