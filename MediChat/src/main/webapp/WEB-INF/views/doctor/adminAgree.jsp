<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<div>
	<h2>의사회원가입 승인(관리자)</h2>
	<hr size="1" width="80%" noshade="noshade">
	<table>
		<tr>
			<th>이름</th>
			<th>의사 면허증</th>
			<th>가입일</th>
			<th>승인</th>
		</tr>
		<c:forEach var="doc" items="${docList}">
		<tr>
			<td>${doc.mem_name}</td>
			<td><img src="${pageContext.request.contextPath}/upload/${doc.doc_license}"></td>
			<td>${doc.doc_reg}</td>
			<td><input type="button" value="승인" id="agree-btn" data-doc="${doc.doc_num}"></td>
		</tr>
		</c:forEach>
	</table>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-3.7.1.min.js"></script>
<script type="text/javascript">
	$(document).ready(function(){
		$('#agree-btn').click(function(){
			var doc_num = $(this).data('doc');
			var $row = $(this).closest('tr');
			$.ajax({
				url:'/updateAgree',
				type:'post',
				data:{doc_num:doc_num},
				dataType:'json',
				success:function(param){
					alert('회원가입이 승인되었습니다.');
					$row.remove();
				},
				error:function(){
					alert('승인 오류');
				}
			});
		});
	});
</script>
</div>