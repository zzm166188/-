app.controller("indexController",function($scope,loginService){

	//显示当前登陆人
	$scope.showName = function(){
		loginService.showName().success(function(response){
			$scope.loginName = response.username;//Map  POJO
			$scope.time = response.curTime;
		});
	}
	
});