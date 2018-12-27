app.controller('payController' ,function($scope ,$location,payService){
	
	//生成微信支付的二维码
	//静态二维码: 转帐给谁 转多少钱没说  填写金额  提交准备好
	//动态二维码: 转帐给谁 转多少钱说了  无权填写金额 无法提前准备好  当场生成
	$scope.createNative=function(){
		payService.createNative().success(
			function(response){
				
				//显示订单号和金额
				$scope.money= (response.total_fee/100).toFixed(2);
				$scope.out_trade_no=response.out_trade_no;
				
				//生成二维码
				 var qr=new QRious({
					    element:document.getElementById('qrious'),
						size:250,
						value:response.code_url,//传智播客: 有权调用  腾讯公司给的地址  微信扫一扫 就可以付钱  收款方是谁 金额是多少
						level:'H'
			     });
				 
				 queryPayStatus();//调用查询
				
			}	
		);	
	}
	
	//调用查询
	queryPayStatus=function(){
		payService.queryPayStatus($scope.out_trade_no).success(
			function(response){
				if(response.success){
					location.href="paysuccess.html#?money="+$scope.money;
				}else{
					if(response.message=='二维码超时'){
						$scope.createNative();//重新生成二维码
					}else{
						location.href="payfail.html";
					}
				}				
			}		
		);		
	}
	
	//获取金额
	$scope.getMoney=function(){
		return $location.search()['money'];
	}
	
});