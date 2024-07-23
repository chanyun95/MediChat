package kr.spring.chat.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import kr.spring.chat.service.ChatService;
import kr.spring.chat.vo.ChatFileVO;
import kr.spring.chat.vo.ChatMsgVO;
import kr.spring.chat.vo.ChatPaymentVO;
import kr.spring.chat.vo.ChatVO;
import kr.spring.doctor.service.DoctorService;
import kr.spring.doctor.vo.DoctorVO;
import kr.spring.member.service.MemberService;
import kr.spring.member.vo.MemberVO;
import kr.spring.reservation.service.ReservationService;
import kr.spring.reservation.vo.ReservationVO;
import kr.spring.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class ChatController {
	@Autowired
	ChatService chatService;
	
	@Autowired
	MemberService memberService;
	
	@Autowired
	DoctorService doctorService;
	
	@Autowired
	ReservationService reservationService;
	
	/*=======================
	 * 	 비대면채팅 페이지 호출
	 ========================*/
	@GetMapping("/chat/chatView")
	public String getChat(HttpSession session, Model model) {
		Object user = session.getAttribute("user");
		
		if(user == null) {
			model.addAttribute("message","로그인이 필요합니다.");
			model.addAttribute("url","/member/login");
			return "/common/resultAlert";
		}
		
		List<ChatVO> list = new ArrayList<ChatVO>();
		
		
		if (user instanceof DoctorVO) {
            DoctorVO doctor = (DoctorVO) user;
            model.addAttribute("user", doctor);
            list = chatService.selectChatListForDoc(doctor.getMem_num());
			log.debug("<<생성된 채팅방 호출>> - 의사회원번호: " + doctor.getMem_num());
			log.debug("<<생성된 채팅방 호출>> - 일반회원번호: "+list);
        } else if (user instanceof MemberVO) {
            MemberVO member = (MemberVO) user;
            model.addAttribute("user", member);
            list = chatService.selectChatListForMem(member.getMem_num());
			log.debug("<<생성된 채팅방 호출>> - 일반회원번호: "+member.getMem_num());
			log.debug("<<생성된 채팅방 호출>> - 일반회원번호: "+list);
        }
		
		model.addAttribute("list",list);
		log.debug("생성된 채팅방의 모델: "+model);
		
		return "chatView";
	}
	
	/*=======================
	 * 	  채팅 내역 불러오기
	 ========================*/
	@GetMapping("/chat/chatDetail")
	@ResponseBody
	public Map<String, Object> getMessage(HttpSession session,
										  Model model,
										  @RequestParam("chat_num") long chat_num,
										  @RequestParam("res_date") String res_date,
										  @RequestParam("res_time") String res_time){
		log.debug("<<채팅 입장>> 채팅방 번호: "+ chat_num);
		log.debug("<<예약 날짜, 예약 시간: >>"+res_date+ res_time);
		
		Map<String,Object> map = new HashMap<String,Object>();
		Object user = session.getAttribute("user");
		
		if(user==null) {
			map.put("userCheck", "logout");
		}else {
			map.put("userCheck", "login");
		}
		
		
		if (user instanceof MemberVO) {
			MemberVO member = (MemberVO) user;
			model.addAttribute("user", member);
			int userAuth = member.getMem_auth();
			if(userAuth == 1) {
				//현재 로그인한 사용자가 정지회원인 경우
	        	map.put("type","1");
			}
		}else {
			DoctorVO doctor = (DoctorVO) user;
	        model.addAttribute("user", doctor);
	        int userAuth = doctor.getMem_auth();
	        if(userAuth == 1) {
	        	//현재 로그인한 사용자가 정지회원인 경우
	        	map.put("type","1");
	        }
		};
		
		Object userType = model.getAttribute("user");
		
		List<ChatMsgVO> msg_list = new ArrayList<ChatMsgVO>();
		ReservationVO reservation = chatService.selectReservationByChatNum(chat_num);
		
		//파라미터를 날짜 및 시각으로 파싱
		LocalDate resDate = LocalDate.parse(reservation.getRes_date(), DateTimeFormatter.ISO_DATE);
		LocalTime resTime = LocalTime.parse(reservation.getRes_time(), DateTimeFormatter.ofPattern("HH:mm"));
		
		//local 시각과 비교할 날짜와 시각으로 파싱
		LocalDateTime reservationDateTime = LocalDateTime.of(resDate, resTime);
		
		if(userType instanceof MemberVO) {
			//현재 로그인한 사용자가 일반회원인 경우
			map.put("type", "2");
		}else if(userType instanceof DoctorVO) {
			//현재 로그인한 사용자가 의사회원인 경우
			map.put("type", "3");
		}
		
		if(LocalDateTime.now().isAfter(reservationDateTime)){
			log.debug("<<채팅방 사용 가능>> - 예약 날짜/시간: "+resDate+"/"+resTime);
			msg_list = chatService.selectMsg(chat_num);
			map.put("res_num",reservation.getRes_num());
			map.put("res_date",res_date);
			map.put("res_time",res_time);
			map.put("list",msg_list);
			map.put("chat","open");
		}else{
			log.debug("<<채팅방 사용 불가>> - 예약 날짜/시간: "+resDate+"/"+resTime);
			map.put("chat","close");
		}
		
		model.addAttribute("chat_num",chat_num);
		
	 return map;
		
	}
	
	//예약 확정 버튼 클릭 시 채팅방 생성
	//@PostMapping("")
	//public void submitRes(ReservationVO reservationVO) {
		
		
	//}
	
	/*=======================
	 * 	 	 메시지 입력
	 ========================*/
	@PostMapping("/chat/chatRoom")
	@ResponseBody
	public Map<String,Object> insertMsg(HttpSession session,
										ChatMsgVO chatMsgVO){
		
		Map<String,Object> map = new HashMap<String,Object>();
		
		Object user = session.getAttribute("user");
		
		if(user==null) {
			map.put("userCheck", "logout");
		}else {
			map.put("userCheck", "login");
		}
		
		if(user instanceof MemberVO) {
			chatMsgVO.setMsg_sender_type(0); //일반회원이 0
			chatService.insertMsg(chatMsgVO);
		}else if(user instanceof DoctorVO) {
			chatMsgVO.setMsg_sender_type(1); //의사회원이 1
			chatService.insertMsg(chatMsgVO);
			}

		return map;
	}
	
	/*=======================
	 * 	   이미지 폼 전송
	 ========================*/
	//계속..수정...
	@PostMapping("image_input")
	@ResponseBody
	public Map<String,Object> insertImage(@RequestParam("select_image") MultipartFile select_image,
										  HttpSession session,
										  HttpServletRequest request,
										  ChatMsgVO chatMsgVO)
										  throws Exception{
		Map<String,Object> map = new HashMap<String,Object>();
		
		Object user = session.getAttribute("user");
		
		if(user==null) {
			map.put("userCheck", "logout");
		}else {
			map.put("userCheck", "login");
		}
		
		String image_name = FileUtil.createFile(request, select_image);
		byte[] image = FileUtil.getBytes(request.getServletContext().getRealPath(image_name));
		
		chatMsgVO.setMsg_content("<img src="+image+"class='upload-image'>");
		
		if(user instanceof MemberVO) {
			chatMsgVO.setMsg_sender_type(0); //일반회원이 0
			chatService.insertImage(chatMsgVO);
		}else if(user instanceof DoctorVO) {
			chatMsgVO.setMsg_sender_type(1); //의사회원이 1
			chatService.insertImage(chatMsgVO);
			}

		return map;
	}
	
	/*=======================
	 * 	   진료 파일 전송
	 ========================*/
	@PostMapping("/chat/chatClose")
	@ResponseBody
	public Map<String,Object> insertChatFile(@ModelAttribute ChatFileVO chatFileVO,
											 @RequestParam("select_file") MultipartFile select_file,
											 HttpServletRequest reqeust,
											 HttpSession session
							   				)throws IOException{
		
		log.debug("<<파일 전송 컨트롤러 진입>>");		
		ChatVO chat = chatService.selectChat(chatFileVO.getChat_num());
	
		//chat_num을 기준으로 일반 회원 번호와 의사 회원 번호를 저장
		chatFileVO.setMem_num(chat.getMem_num());
		chatFileVO.setDoc_num(chat.getDoc_num());
		
		String file_name = FileUtil.createFile(reqeust,select_file);
		
		int indexFileName = file_name.indexOf("_");
		String origin_file_name = file_name.substring(indexFileName+1);
		
		log.debug("<<전송한 파일의 이름 생성>>: "+file_name);
		
		chatFileVO.setFile_name(file_name);
		
		Map<String,Object> map = new HashMap<String,Object>();

		if(chatFileVO.getFile_valid_date()!=null||!chatFileVO.getFile_valid_date().isEmpty()){
			//validDate : file_valid_date String -> Date로 변환한 값
			LocalDate validDate = LocalDate.parse(chatFileVO.getFile_valid_date(), DateTimeFormatter.ISO_DATE);			
			
			if(LocalDate.now().isAfter(validDate)){
				//유효기간을 지난 날짜로 설정한 경우
				map.put("valid_date","pastDate");
			}else{
				//DB에 데이터 저장
				chatService.insertChatFile(chatFileVO);
			
				map.put("valid_date", chatFileVO.getFile_valid_date());
				map.put("file_name", origin_file_name);
				
				//파일 타입 반환
				switch (chatFileVO.getFile_type()) {
                case 0:
                    map.put("file_type", "처방전");
                    break;
                case 1:
                    map.put("file_type", "진단서");
                    break;
                case 2:
                    map.put("file_type", "소견서");
                    break;
                case 3:
                    map.put("file_type", "진료비 세부내역서");
                    break;
				}
				
				Long file_num = chatService.selectFileNum(chatFileVO.getChat_num(), file_name);
				map.put("file_num", file_num);
				
				log.debug("<<반환한 file_num>>:"+file_num);
				
			}//end of else
		}//end of not "pastDate"
		return map;
	}
	
	/*=======================
	 * 	   진료 파일 삭제
	 ========================*/
	@PostMapping("/chat/deleteFile")
	@ResponseBody
	public Map<String,Object> deleteFile(@RequestParam("file_num") long file_num){
		Map<String,Object> map = new HashMap<String,Object>();
		
		try {
			chatService.deleteFile(file_num);
			map.put("result", "success");
		}catch(Exception e) {
			log.debug("<<파일 삭제 오류>>:"+e);
			map.put("result", "fail");
		}
		
		return map;
	}


	
	/*=======================
	 * 	    진료 파일 목록
	 ========================*/
	@GetMapping("/chat/myFiles")
	public String selectFiles(Model model, HttpSession session) {
		//해당 페이지는 환자만 들어올 수 있으므로 회원 등급 조회 x
		MemberVO user = (MemberVO)session.getAttribute("user");
		
		if(user==null) {
			model.addAttribute("message","로그인이 필요합니다.");
			model.addAttribute("url","/member/login");
			return "/common/resultAlert";
		}
		
		
		long mem_num = user.getMem_num();
		
		List<ChatVO> chat = chatService.selectChatListForMem(mem_num);
		
		model.addAttribute("chat",chat);
		
		return "chatMyFiles";
	}
	
	/*=======================
	 * 	    진료 파일 목록
	 ========================*/
	@GetMapping("/chat/fileDetail")
	@ResponseBody
	public Map<String,Object> selectFilesDetail(HttpSession session, long chat_num){
		Map<String,Object> map = new HashMap<String,Object>();
		
		MemberVO user = (MemberVO)session.getAttribute("user");
		
		if(user == null) {
			map.put("userCheck", "logout");
		}
		
		List<ChatFileVO> list = chatService.selectFiles(chat_num);
		
		
		if(list == null || list.isEmpty()) {
			map.put("list", "null");
		}else {
			map.put("list", list);
		}
		
		return map;
	}
	
	/*=======================
	 * 	    진료 종료 전송
	 ========================*/
	@GetMapping("/chat/downloadFile")
	public String download(long file_num, HttpServletRequest request, Model model) {
		ChatFileVO file = chatService.selectFile(file_num);
		
		byte[] downloadFile = FileUtil.getBytes(request.getServletContext().getRealPath("/upload")+"/"+file.getFile_name());
		
		String file_name = file.getFile_name();
		
		int indexFileName = file_name.indexOf("_");
		String origin_file_name = file_name.substring(indexFileName+1);
		
		log.debug("<<파일 다운로드>>: "+origin_file_name);
		
		model.addAttribute("downloadFile",downloadFile);
		model.addAttribute("filename",origin_file_name);
		
		return "downloadView";
	}
	

	/*=======================
	 * 	    진료 종료 전송
	 ========================*/
	@PostMapping("/chat/requestPayment")
	@ResponseBody
	public Map<String,Object> requestPayment(@RequestParam("chat_num") long chat_num,
											 @RequestParam("pay_amount") int pay_amount
											 ){
		Map<String,Object> map = new HashMap<String,Object>();
		
		ChatVO chat = chatService.selectChat(chat_num);
		log.debug("<<채팅 번호>>:"+chat_num);
		
		ReservationVO res = chatService.selectReservationByChatNum(chat_num);
		long res_num = chat.getRes_num();
		log.debug("<<예약 번호>>:"+res_num);
		
		String res_date = res.getRes_date();
		String res_time = res.getRes_time();
		
		//환자 번호 구하기
		long mem_num = chat.getMem_num();
		log.debug("<<환자 번호>>:"+mem_num);
		
		//의사 번호 구하기
		long doc_num = chat.getDoc_num();
		log.debug("<<의사 번호>>:"+doc_num);
		
		//환자 이름 구하기
		MemberVO member = memberService.selectMember(mem_num);
		String mem_name = member.getMem_name();
		log.debug("<<환자 이름>>:"+mem_name);
		
		//의사 이름 구하기
		DoctorVO doctor = doctorService.selectDoctor(doc_num);
		String doc_name = doctor.getMem_name();
		log.debug("<<의사 이름>>:"+doc_name);
		
		try {
			String paymentNotice = "예약번호:" + res_num + "의 진료비 청구가 도착했습니다.";
			paymentNotice += "<br>환자 성명: "+ mem_name;
			paymentNotice += "<br>담당 의사: "+ doc_name;
			paymentNotice += "<br>진료 일자: "+ res_date;
			paymentNotice += "<br>진료 시각: "+ res_time;
			paymentNotice += "<br>결제 금액: "+ pay_amount;
			paymentNotice += "<br><button type='button' class='btn-green' id='chat_payment' ";
			paymentNotice += "data-chat_num='"+chat_num+"' data-pay_amount='"+pay_amount+"'>";
			paymentNotice += "결제하기</button>";
			
			map.put("paymentNotice", paymentNotice);
			
			ChatMsgVO paymentNoticeMSG = new ChatMsgVO();
			paymentNoticeMSG.setChat_num(chat_num);
			paymentNoticeMSG.setMsg_content(paymentNotice);
			paymentNoticeMSG.setMsg_sender_type(1);
			
			chatService.insertMsg(paymentNoticeMSG);
			map.put("result", "success");
			
		}catch(Exception e) {
			map.put("result", "fail");
		}
		
		return map;
	}
	
	
	/*=======================
	 * 	   	결제 버튼 클릭
	 ========================*/
	@GetMapping("/chat/chatPayment")
	@ResponseBody
	public Map<String,Object> insertPayment(@RequestParam("chat_num") long chat_num){
		
		log.debug("<<비대면 진료 결제 chat_num>>: "+chat_num);
		Map<String,Object> map = new HashMap<String,Object>();
		
		ChatPaymentVO payment = new ChatPaymentVO();
		
		ChatVO chat = chatService.selectChat(chat_num);
		payment.setChat_num(chat.getChat_num());
		payment.setMem_num(chat.getMem_num());
		
		MemberVO member = memberService.selectMember(chat.getMem_num());
		payment.setMem_phone(member.getMem_phone());
		DoctorVO doctor = doctorService.selectDoctor(chat.getDoc_num());
		
		String doc_name = doctor.getMem_name();
		
		map.put("payment", payment);
		map.put("doc_name", doc_name);
		
		return map;
	}
	
	/*=======================
	 * 	   	결제 완료 처리
	 ========================*/
	@PostMapping("/chat/paymentConfirmation")
	public Map<String,String> confirmPayment(@RequestParam("chat_num") long chat_num,
									         @RequestParam("doc_name") String doc_name,
									         @RequestParam("pay_amount") int pay_amount,
									         @RequestParam("mem_num") long mem_num) {
	    
	    Map<String, String> map = new HashMap<String,String>();
	    
	    ChatPaymentVO payment = new ChatPaymentVO();
	    
	    payment.setChat_num(chat_num);
	    payment.setMem_num(mem_num);
	    payment.setPay_amount(pay_amount);
	    payment.setDoc_name(doc_name);
	    
	    try {
	        chatService.insertChatPayment(payment);
	        map.put("result", "paySuccess");
	    } catch (Exception e) {
	        map.put("result", "fail");
	        // 로그에 예외 메시지를 기록
	        e.printStackTrace();
	    }
	    
	    return map;
	}
}
