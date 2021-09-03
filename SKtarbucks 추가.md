# SKtarbucks 추가


# Table of contents

- 서비스 시나리오
- 분석/설계
  - Event Storming
  - 헥사고날 아키텍처 다이어그램 도출
- 구현
  - 시나리오 테스트결과
  - DDD의 적용
  - Gateway 적용
  - 폴리글랏 퍼시스턴스
  - 동기식 호출과 Fallback 처리
  - 비동기식 호출과 Eventual Consistency
 - 운영
   - CI/CD 설정
   - Self-healing
   - 무정지 재배포
   - ConfigMap 사용

- 1차 팀과제 (https://github.com/rlatjdwo555/SKtarbucks)
- 결재/배달/푸쉬서비스 추가

# 서비스 시나리오 확장 

## 기존 시나리오
1. 카페주인이 카페 메뉴(음료이름,주만가능일, 주문가능수)를 등록한다.
2. 고객이 주문을 요청한다. <br>
 2-1. 카페에서 주문가능 수를 확인한다. <br>
3. 구객의 주문요청에 따라 해당 음료의 주문가능 수가 감소한다. <br>
 
4. 주문이 완료되면 주문관리내 해당 내역의 상태가 등록된다.
5. 고객이 주문을 취소한다. <br>
 5-1. 고객의 주문취소에 따라 음료의 주문가능 수가 증가한다. <br>
6. 고객의 주문 취소에 따라서 주문관리의 해당 내역의 주문상태가 취소로 변경된다. <br> 
7. 카페주인이 메뉴 정보를 삭제한다. <br>
8. 카페주인의 주문 정보 삭제에 따라 고객의 해당 메뉴 주문상태가 취소로 변경된다. 
 
## 추가 시나리오 (3개 서비스 추가)
1. 카페주인이 카페 메뉴를 등록한다. 
2. 고객이 주문을 한다. 

3. 결제 서비스에 데이터가 추가된다. <br>
 3-1. 결제가 승인되면 주문관리 서비스에 데이터가 추가된다. <br>
4. 결제가 취소되면 주문이 취소되고, 주문가능 수가 증가한다. <br>
 
5. 고객이 주문을 취소한다. <br>
 5-1. 주문상태/결재상태/제조상태가 취소로 변경된다. <br>
 5-2. 고객의 주문취소에 따라 음료의 주문가능 수가 증가한다. <br>

6. 음료 제조가 시작되면 사용자에게 제조시작 푸쉬 알람을 보낸다.
7. 바리스타가 음료 제조를 완료한다.<br>
 7-1. 주문타입이 'DELIVERY"이면 배달 서비스에 데이터를 등록한다. <br> 

8. 음료가 제작완료되어 배달원이 배달을 출발한다. <br>
 8-1. 고객에게 배달시작 푸쉬 알람을 보낸다. <br>
 8-2. 주문상태가 배달중 으로 변경된다. <br> 

9. 배달원이 배달을 완료한다. <br> 
 9-1. 고객에게 배달완료 푸쉬 알람을 보낸다. <br>
 9-2. 주문상태가 완료로 변경된다. <br> 


# 분석/설계

## 기존 Event Storming
![eventstorming](https://i.imgur.com/RH6rs40.png)


## 확장 Event Storming
![모델링](https://user-images.githubusercontent.com/28692938/126950447-d305ffee-9160-433e-823d-e3ae627ffe57.PNG)



## 헥사고날 아키텍처 다이어그램 도출

### 기존 다이어그램

- CQRS 를 위한 Mypage 서비스만 DB를 구분하여 적용 <br>
![image](https://i.imgur.com/XLlfwa9.png)



### 확장 다이어그램
- 3대 서비스 추가(Payment, Delivery, Push) 
- MyPage에서 신규 서비스의 Event를 수신하도록 구현 
![image](https://i.imgur.com/M22X2l5.png)



# 구현

## 시나리오 테스트결과


#### 1.카페 주인이 카페메뉴(메뉴명, 가격, 주문가능 수)를 등록한다.
```
http localhost:8082/cafes cafeNm="Americano" price=3000 stock=10
http localhost:8082/cafes cafeNm="Latte" price=5500 stock=5
http localhost:8082/cafes cafeNm="Coldblue" price=6000 stock=10
http localhost:8082/cafes cafeNm="Juice" price=9000 stock=10

http localhost:8082/cafes
```
![카페메뉴 등록](https://i.imgur.com/ysJyBry.png)



#### 2. 고객이 주문을 요청한다.
- 주문타입은 배달로 요청한다. <br>
orderType="`DELIVERY`"
```
http localhost:8081/orders cafeId=2 custNm="HSI" count=3 orderType="DELIVERY"
http localhost:8081/orders cafeId=1 custNm="HSY" count=1 orderType="DELIVERY"
```
![주문1](https://i.imgur.com/NPUeq1G.png)

#### 3. 결제 서비스에 데이터가 추가된다.
- 결제 상태값이 결제 요청됨으로 등록된다. <br>
paymentStatus = "`PAYMENT_REQUESTED`"
```
http localhost:8085/payments
```
![주문1-결제1](https://i.imgur.com/YHjyDDe.png)

- 결제가 승인되면 제조관리 서비스에 데이터가 추가된다.
```
http PATCH localhost:8085/payments/1 paymentStatus="APPROVED"

http localhost:8083/productions
```
![주문1-결제승인](https://i.imgur.com/CX7EAow.png)

![주문1-제조](https://i.imgur.com/ur738uU.png)


#### 4. 바리스타가 음료를 제조한다.
 - 제조가 시작되면 고객에게 푸쉬 알람을 보낸다.
 - 주문의 상태가 변경된다.
 orderStatus = `MAKING`
 ```
 http localhost:8087/pushes
 
 http localhost:8081/order/1
 ```
 ![주문1-제조푸쉬](https://i.imgur.com/MZgHxIb.png)
 
 ![주문1-제조 상태 변경](https://i.imgur.com/2BbTc4F.png)
 
#### 5. 바리스타가 음료 제조를 완료한다.
- 주문타입이 '`DELIVERY`' 만 배달 서비스에 데이터를 등록한다.
```
http PATCH localhost:8083/productions/1 status="COMPLETED"

http localhost:8086/deliveries
```
![주문1-제조완료](https://i.imgur.com/hYjYD9s.png)

![주문1-배달시작](https://i.imgur.com/tRKgVLY.png)


#### 6. 배달원이 배달을 시작한다.
- 고객에게 배달 시작 푸쉬 알람을 보낸다.
- 주문의 상태값이 **배달 중**으로 변경된다.
orderStatus = "`DELIVERY_IN_PROGRESS`"
```
http localhost:8087/pushes
http localhost:8081/orders/1
```
![주문1-배달시작 푸쉬](https://i.imgur.com/NYVEBGF.png)

![주문1-상태변경-배달중](https://i.imgur.com/L0Xm5D0.png)

#### 7. 배달을 완료한다.
- 고객에게 배달 완료 푸쉬 알람을 보낸다.
- 주문의 상태값이 **배달 완료**로 변경된다.
orderStatus = '`DELIVERY_COMPLETED`'
```
http PATCH localhost:8086/deliveries/1 status="COMPLETED"

http localhost:8087/pushes
http localhost:8081/orders/1
```
![주문1-배달완료](https://i.imgur.com/yBEeTGr.png)

![주문1-배달완료 푸쉬](https://i.imgur.com/29fxMIf.png)

![주문1-상태변경-배달완료](https://i.imgur.com/tSpTH34.png)


#### 8.주문 후 결제 여부에 따라 주문 가능 수량이 변경된다.
- 최초 주문 가능 수량을 확인한다. (Juice : 10개)
```
http localhost:8082/cafes/5
```
![3번메뉴 수량](https://i.imgur.com/8DYlZsP.png)

- 주문 요청 시 주문 가능 수량이 감소한다.
```
http localhost:8081/orders cafeId=5 custNm="HSI" count=1 orderType="DELIVERY"

http localhost:8082/cafes/5
```
![주문3](https://i.imgur.com/XCb4yKD.png)

![3번메뉴 수량 5](https://i.imgur.com/j0lTKoo.png)

- 결제 취소 시 주문이 취소되며 주문 가능 수량이 증가한다.
- 주문 상태값이 취소로 변경된다. <br>
orderStatus = "`CANCELED`"
```
http PATCH localhost:8085/payments/1 paymentStatus="CANCELED"

http localhost:8082/cafes
http localhost:8081/orders
```
![주문3-결제취소](https://i.imgur.com/gCZbbNj.png)

![주문3-결제취소](https://i.imgur.com/B9x3HXG.png)

![3번메뉴 수량 10](https://i.imgur.com/FQr6tkq.png)

![주문3-주문상태-결제취소](https://i.imgur.com/F55fAph.png)

#### 9.고객이 주문을 취소한다.
```
http PATCH localhost:8081/orders/2 status="CANCELED"
```
![주문4-주문취소](https://i.imgur.com/JLsMTpI.png)

![주문4-주문취소](https://i.imgur.com/Sq3VQ9K.png)

- 해당 주문의 결제 상태값이 취소로 변경된다.
paymentStatus = "`PAYMENT_CANCELED`"

```
http localhost:8085/payments/2
```
![주문4-주문취소-결제취소](https://i.imgur.com/Ka8UCo2.png)

- 주문 내역의 음료 주문가능 수가 증가한다.

![메뉴4](https://i.imgur.com/FQr6tkq.png)


## DDD의 적용

3개의 서비스 추가 (Payment, Delivery, Push)
* MyPage 는 CQRS 를 위한 서비스

| MSA | 기능 | port | 조회 API | Gateway 사용시 |
|---|:---:|:---:|---|---|
| Order | 주문 관리 | 8081 | http://localhost:8081/orders | http://OrderManage:8080/orders |
| Cafe  | 카페메뉴 관리 | 8082 | http://localhost:8082/cafes | http://CafeManage:8080/cafes |
| Production | 제조 관리 | 8083 | http://localhost:8083/productions | http://ProductionManage:8080/productions |
| MyPage | 마이페이지 | 8084 | http://localhost:8084/myPages | http://MyPage:8080/myPages |
| Payment | 결제 관리 | 8085 | http://localhost:8085/payments | http://Payment:8080/payments |
| Delivery  | 배달 관리 | 8086 | http://localhost:8086/deliveries | http://DeliveryManage:8080/deliveries |
| Push | 푸쉬알람 관리 | 8087 | http://localhost:8087/pushes | http://PushManage:8080/pushes |


## Gateway 적용

```
spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: OrderManage
          uri: http://localhost:8081
          predicates:
            - Path=/orders/**
        - id: CafeManage
          uri: http://localhost:8082
          predicates:
            - Path=/cafes/** 
        - id: ProductionManage
          uri: http://localhost:8083
          predicates:
            - Path=/productions/** 
        - id: MyPage
          uri: http://localhost:8084
          predicates:
            - Path= /myPages/**
        - id: Payment
          uri: http://localhost:8085
          predicates:
            - Path= /payments/**
        - id: Delivery
          uri: http://localhost:8086
          predicates:
            - Path= /deliveries/**
        - id: PushManage
          uri: http://localhost:8087
          predicates:
            - Path= /pushes/**                
...
```


## 폴리글랏 퍼시스턴스

CQRS 를 위한 Mypage 서비스만 DB를 구분하여 적용함. 인메모리 DB인 hsqldb 사용.

```
pom.xml 에 적용
<!-- 
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
 -->
		<dependency>
		    <groupId>org.hsqldb</groupId>
		    <artifactId>hsqldb</artifactId>
		    <version>2.4.0</version>
		    <scope>runtime</scope>
		</dependency>
```

- MyPage 조회 결과 
```
http localhost:8084/myPages
```
![myPage](https://i.imgur.com/9jpQ8rw.png)



## 동기식 호출과 Fallback 처리
제조 서비스에서 주문 타입을 확인할 때 제조(Production)->주문(Order) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리

- FeignClient 서비스 구현 
- REST url을 config-map 으로 구현 

```java
# OrderService.java

@FeignClient(name="OrderManage", url="${api.order.url}")
public interface OrderService {

    @RequestMapping(method= RequestMethod.GET, value="/orders/{orderId}", consumes = "application/json")
    public Order getOrderType(@PathVariable("orderId") Long orderId);

}
```

- 제조 완료처리를 한 후 Order의 주문 타입을 확인 
```java
# Production.java

@PostUpdate
public void onPostUpdate(){
    if("COMPLETED".equals(status)){
    
        local.external.Order order = new local.external.Order();
        
        order = ProductionManageApplication.applicationContext.getBean(local.external.OrderService.class)
        .getOrderType(orderId);
           
        switch (order.getOrderType()) {
            case "TAKEOUT":
            ProductionChanged productionChanged = new ProductionChanged();
            BeanUtils.copyProperties(this, productionChanged);
            productionChanged.publishAfterCommit();                            
            break;

            case "DELIVERY":
            ProductionSent productionSent = new ProductionSent();
            BeanUtils.copyProperties(this, productionSent);
            productionSent.publishAfterCommit();    
            break;
           
            default :              
        }
    }
}   
```



## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

푸쉬 알람 서비스는 비동기식으로 구현하였고 푸쉬 서비스가 내려가 있더라도 다른 서비스에는 영향을 주지 않는다.

- Push 서비스가 내려간 상태에서 주문 및 제조 완료 후, 서비스 재기동시 이벤트 수신을 확인한다.




# 운영

## CI/CD 설정

각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 AWS CodeBuild를 사용하였으며, 
pipeline build script 는 각 프로젝트 폴더 이하에 buildspec.yml 에 포함되었다.
- CodeBuild 기반으로 CI/CD 파이프라인 구성
MSA 서비스별 CodeBuild 프로젝트 생성하여  CI/CD 파이프라인 구성

![ECR](https://i.imgur.com/baPB4z9.png)

![CodeBuild](https://i.imgur.com/Tuagb9C.png)


- Git Hook 연결
연결한 Github의 소스 변경 발생 시 자동으로 빌드 및 배포 되도록 Git Hook 연결 설정

![GitHook](https://i.imgur.com/Ui61JVz.png)

![livenessProve](https://i.imgur.com/H5KPP7Y.png)

## Self-healing (Liveness Probe)
Pod는 정상적으로 작동하지만 내부의 어플리케이션이 반응이 없다면, 컨테이너는 의미가 없다.
위와 같은 경우는 어플리케이션의 Liveness probe는 Pod의 상태를 체크하다가, Pod의 상태가 비정상인 경우 kubelet을 통해서 재시작한다.



## 무정지 재배포 (ReadinessProve)

- buildspec.yaml에 ReadinessProbe 설정 
```
# Readiness probe 설정

readinessProbe:
    httpGet:
      path: '/actuator/health'
      port: 8080
    initialDelaySeconds: 30
    timeoutSeconds: 2
    periodSeconds: 5
    failureThreshold: 10
```

- CI/CD 파이프라인을 통해 새버전으로 재배포 작업 간 siege 모니터링 



## ConfigMap 사용

시스템별로 또는 운영중에 동적으로 변경 가능성이 있는 설정들을 ConfigMap을 사용하여 관리합니다.
Application에서 특정 도메일 URL을 ConfigMap 으로 설정하여 운영/개발등 목적에 맞게 변경가능합니다.  



* Production/buildsepc.yaml 내 ConfigMap 정의
```
...
      - |
        cat <<EOF | kubectl apply -f -
        apiVersion: v1
        kind: ConfigMap
        metadata:
          name: my-config2
          namespace: $_NAMESPACE
        data:
          api.order.url: http://OrderManage:8080
        EOF
      - |
            ...
                  env:
                    - name: api.order.url
                      valueFrom:
                        configMapKeyRef:
                          name: my-config2
                          key: api.order.url 
...
```




