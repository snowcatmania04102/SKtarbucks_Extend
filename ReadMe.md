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

# 서비스 시나리오


 
## 기능적 요구사항
1. 카페주인이 카페 메뉴를 등록한다. 
2. 고객이 주문을 한다. 
2-1. 카페에서 주문가능 수를 확인한다.
2-2. 음료의 주문가능 수가 감소한다. -> (Sync)

3. 결제 서비스에 데이터가 추가된다. <br>
 3-1. 결제가 승인되면 주문관리 서비스에 데이터가 추가된다. <br>
4. 결제가 취소되면 주문이 취소되고, 주문가능 수가 증가한다. <br>
 
5. 고객이 주문을 취소한다. <br>
 5-1. 주문상태/결재상태/제조상태가 취소로 변경된다. <br>
 5-2. 고객의 주문취소에 따라 음료의 주문가능 수가 증가한다. ->(Async) <br>

6. 음료 제조가 시작되면 사용자에게 제조시작 푸쉬 알람을 보낸다.
7. 바리스타가 음료 제조를 완료한다.<br>
 7-1. 주문타입이 'DELIVERY"이면 배달 서비스에 데이터를 등록한다. <br> 

8. 음료가 제작완료되어 배달원이 배달을 출발한다. <br>
 8-1. 고객에게 배달시작 푸쉬 알람을 보낸다. <br>
 8-2. 주문상태가 배달중 으로 변경된다. <br> 

9. 배달원이 배달을 완료한다. <br> 
 9-1. 고객에게 배달완료 푸쉬 알람을 보낸다. <br>
 9-2. 주문상태가 완료로 변경된다. <br> 
 
 ## 비기능적 요구사항
1. 트랜잭션
* 고객의 주문에 따라서 음료의 주문가능 수가 감소한다. > Sync
* 고객의 취소에 따라서 음료의 주문가능 수가 증가한다. > Async

2. 장애격리
* Push서비스에 장애가 발생하더라도 주문 및 제조 서비스는 정상적으로 처리 가능하다.  > Async (event-driven)


3. 성능
* 고객은 본인의 주문 상태 및 이력 정보를 Mypage에서 확인할 수 있다. > CQRS


# 분석/설계

# Event Storming 

### 이벤트 도출
![image](https://i.imgur.com/Xd9m7Lu.png)



### 부적격 이벤트 탈락
![image](https://i.imgur.com/e9NoVi6.png)

    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함

## Event Storming 결과
수정한 이벤트스토밍
![모델링](https://user-images.githubusercontent.com/28692938/126950447-d305ffee-9160-433e-823d-e3ae627ffe57.PNG)



### 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증
#

## 기능 요구사항 Coverage


### 시나리오 Converage Check - 01
1. 카페주인이 카페 메뉴를 등록한다. 
![image](https://i.imgur.com/4jfvc9R.png)


### 시나리오 Converage Check - 02
2. 고객이 주문을 한다. 
2-1. 카페에서 주문가능 수를 확인한다.
2-2. 음료의 주문가능 수가 감소한다. -> (Sync)

3. 결제 서비스에 데이터가 추가된다. <br>
 3-1. 결제가 승인되면 주문관리 서비스에 데이터가 추가된다. <br>
4. 결제가 취소되면 주문이 취소되고, 주문가능 수가 증가한다. <br>
 
5. 고객이 주문을 취소한다. <br>
 5-1. 주문상태/결재상태/제조상태가 취소로 변경된다. <br>
 5-2. 고객의 주문취소에 따라 음료의 주문가능 수가 증가한다. ->(Async) 
![image](https://i.imgur.com/tuicT0U.png)


### 시나리오 Converage Check - 03
6. 음료 제조가 시작되면 사용자에게 제조시작 푸쉬 알람을 보낸다.
7. 바리스타가 음료 제조를 완료한다.<br>
 7-1. 주문타입이 'DELIVERY"이면 배달 서비스에 데이터를 등록한다. <br> 

8. 음료가 제작완료되어 배달원이 배달을 출발한다. <br>
 8-1. 고객에게 배달시작 푸쉬 알람을 보낸다. <br>
 8-2. 주문상태가 배달중 으로 변경된다. <br> 

9. 배달원이 배달을 완료한다. <br> 
 9-1. 고객에게 배달완료 푸쉬 알람을 보낸다. <br>
 9-2. 주문상태가 완료로 변경된다. <br> 
![image](https://i.imgur.com/qVny44T.png)


## 비기능 요구사항 Coverage
![image](https://i.imgur.com/ykZdWwk.png)
**1. 트랜젝션**

* 고객의 주문에 따라서 음료의 주문가능 수가 감소한다. > Sync (1)
* 고객의 취소에 따라서 음료의 주문가능 수가 증가한다. > Async (2)


**2. 장애격리**

* Push서비스에 장애가 발생하더라도 주문 및 제조 서비스는 정상적으로 처리 가능하다.  > Async (event-driven)



**3. 성능**

* * 고객은 본인의 주문 상태 및 이력 정보를 Mypage에서 확인할 수 있다. > CQRS




## 헥사고날 아키텍처 다이어그램 도출


![image](https://i.imgur.com/M22X2l5.png)
```
  - 3개의 서비스 추가 (Payment, Delivery, Push)
  - Production 에서 Order의 주문타입을 확인할 때 RESTful Request/Response 로 구현
  - MyPage의 ViewHandler에서 신규 서비스의 Event를 수신하도록 확장구현 
```



# 구현

## 시나리오 테스트결과


#### 1.카페 주인이 카페메뉴(메뉴명, 가격, 주문가능 수)를 등록한다.
```
http localhost:8080/cafes cafeNm="Americano" price=3000 stock=10
http localhost:8080/cafes cafeNm="Latte" price=5500 stock=5
http localhost:8080/cafes cafeNm="Coldblue" price=6000 stock=10
http localhost:8080/cafes cafeNm="Juice" price=9000 stock=10

http localhost:8080/cafes
```
![카페메뉴 등록](https://i.imgur.com/ysJyBry.png)



#### 2. 고객이 주문을 요청한다.
- 주문타입은 배달로 요청한다. <br>
orderType="`DELIVERY`"
```
http localhost:8080/orders cafeId=2 custNm="HSI" count=3 orderType="DELIVERY"
http localhost:8080/orders cafeId=1 custNm="HSY" count=1 orderType="DELIVERY"
```
![주문1](https://i.imgur.com/NPUeq1G.png)

#### 3. 결제 서비스에 데이터가 추가된다.
- 결제 상태값이 결제 요청됨으로 등록된다. <br>
paymentStatus = "`PAYMENT_REQUESTED`"
```
http localhost:8080/payments
```
![주문1-결제1](https://i.imgur.com/YHjyDDe.png)

- 결제가 승인되면 제조관리 서비스에 데이터가 추가된다.
```
http PATCH localhost:8080/payments/1 paymentStatus="APPROVED"

http localhost:8080/productions
```
![주문1-결제승인](https://i.imgur.com/CX7EAow.png)

![주문1-제조](https://i.imgur.com/ur738uU.png)


#### 4. 바리스타가 음료를 제조한다.
 - 제조가 시작되면 고객에게 푸쉬 알람을 보낸다.
 - 주문의 상태가 변경된다.
 orderStatus = `MAKING`
 ```
 http localhost:8080/pushes
 
 http localhost:8080/order/1
 ```
 ![주문1-제조푸쉬](https://i.imgur.com/MZgHxIb.png)
 
 ![주문1-제조 상태 변경](https://i.imgur.com/2BbTc4F.png)
 
#### 5. 바리스타가 음료 제조를 완료한다.
- 주문타입이 '`DELIVERY`' 만 배달 서비스에 데이터를 등록한다.
```
http PATCH localhost:8080/productions/1 status="COMPLETED"

http localhost:8080/deliveries
```
![주문1-제조완료](https://i.imgur.com/hYjYD9s.png)

![주문1-배달시작](https://i.imgur.com/tRKgVLY.png)


#### 6. 배달원이 배달을 시작한다.
- 고객에게 배달 시작 푸쉬 알람을 보낸다.
- 주문의 상태값이 **배달 중**으로 변경된다.
orderStatus = "`DELIVERY_IN_PROGRESS`"
```
http localhost:8080/pushes
http localhost:8080/orders/1
```
![주문1-배달시작 푸쉬](https://i.imgur.com/NYVEBGF.png)

![주문1-상태변경-배달중](https://i.imgur.com/L0Xm5D0.png)

#### 7. 배달을 완료한다.
- 고객에게 배달 완료 푸쉬 알람을 보낸다.
- 주문의 상태값이 **배달 완료**로 변경된다.
orderStatus = '`DELIVERY_COMPLETED`'
```
http PATCH localhost:8080/deliveries/1 status="COMPLETED"

http localhost:8080/pushes
http localhost:8080/orders/1
```
![주문1-배달완료](https://i.imgur.com/yBEeTGr.png)

![주문1-배달완료 푸쉬](https://i.imgur.com/29fxMIf.png)

![주문1-상태변경-배달완료](https://i.imgur.com/tSpTH34.png)


#### 8.주문 후 결제 여부에 따라 주문 가능 수량이 변경된다.
- 최초 주문 가능 수량을 확인한다. (Juice : 10개)
```
http localhost:8080/cafes/5
```
![3번메뉴 수량](https://i.imgur.com/8DYlZsP.png)

- 주문 요청 시 주문 가능 수량이 감소한다.
```
http localhost:8080/orders cafeId=5 custNm="HSI" count=1 orderType="DELIVERY"

http localhost:8080/cafes/5
```
![주문3](https://i.imgur.com/XCb4yKD.png)

![3번메뉴 수량 5](https://i.imgur.com/j0lTKoo.png)

- 결제 취소 시 주문이 취소되며 주문 가능 수량이 증가한다.
- 주문 상태값이 취소로 변경된다. <br>
orderStatus = "`CANCELED`"
```
http PATCH localhost:8080/payments/1 paymentStatus="CANCELED"

http localhost:8080/cafes
http localhost:8080/orders
```
![주문3-결제취소](https://i.imgur.com/gCZbbNj.png)

![주문3-결제취소](https://i.imgur.com/B9x3HXG.png)

![3번메뉴 수량 10](https://i.imgur.com/FQr6tkq.png)

![주문3-주문상태-결제취소](https://i.imgur.com/F55fAph.png)

#### 9.고객이 주문을 취소한다.
```
http PATCH localhost:8080/orders/2 status="CANCELED"
```
![주문4-주문취소](https://i.imgur.com/JLsMTpI.png)

![주문4-주문취소](https://i.imgur.com/Sq3VQ9K.png)

- 해당 주문의 결제 상태값이 취소로 변경된다.
paymentStatus = "`PAYMENT_CANCELED`"

```
http localhost:8080/payments/2
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
http localhost:8080/myPages
```
![myPage](https://i.imgur.com/bFQUuYq.png)


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

           // 주문 타입 확인을 동기식으로 호출
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
- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 주문 관리 시스템이 장애가 나면 요청을 처리하지 못하는 것을 확인
```
#주문관리(Order) 서비스를 내려놓음 
kubectl delete svc ordermanage -n skcc-ns

#제조 완료 요청
http PATCH a45a441e9767e4fb1826162c5a289b9b-310245936.ca-central-1.elb.amazonaws.com:8080/productions/1 status="COMPLETED"  #Fail


#주문 서비스 재기동
kubectl expose deploy ordermanage --port=8080 -n skcc-ns

#제조 완료 요청 처리 
http PATCH a45a441e9767e4fb1826162c5a289b9b-310245936.ca-central-1.elb.amazonaws.com:8080/productions/1 status="COMPLETED" #Success       
```
![myPage](https://i.imgur.com/tD5fU1A.png)


## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

푸쉬 알람 서비스는 비동기식으로 구현하였고 푸쉬 서비스가 내려가 있더라도 다른 서비스에는 영향을 주지 않는다.

- Push 서비스가 내려간 상태에서 주문 및 제조 완료 후, 서비스 재기동시 이벤트 수신을 확인한다.

```
# 푸쉬 서비스 다운 
kubectl delete deploy,svc pushmanage -n skcc-ns

# 주문처리
http a45a441e9767e4fb1826162c5a289b9b-310245936.ca-central-1.elb.amazonaws.com:8080/orders cafeId=1 custNm="HSI" count=1 orderType="TAKEOUT"

# 결제 승인 및 제조완료 처리
http PATCH a45a441e9767e4fb1826162c5a289b9b-310245936.ca-central-1.elb.amazonaws.com:8080/payments/2 paymentStatus="APPROVED"
http PATCH a45a441e9767e4fb1826162c5a289b9b-310245936.ca-central-1.elb.amazonaws.com:8080/productions/1 status="COMPLETED"

# 푸쉬 서비스 재기동 후 Event 수신 확인
kubectl create deployment pushmanage --image=879772956301.dkr.ecr.ca-central-1.elb.amazonaws.com/user23-pushmanage:latest -n skcc-ns
kubectl expose deploy pushmanage --port=8080 -n skcc-ns

```
![myPage](https://i.imgur.com/2I61qCi.png)

![myPage](https://i.imgur.com/id1fUp4.png)

![myPage](https://i.imgur.com/smuMDaL.png)


- 음료 제조상태 변경 이벤트를 카프카로 송출한다(Publish)
```java
...
    @PostPersist
    public void onPostPersist(){
        ProductionChanged productionChanged = new ProductionChanged();
        BeanUtils.copyProperties(this, productionChanged);
        productionChanged.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate(){
        if("COMPLETED".equals(status)){

           // 주문 타입 확인을 동기식으로 호출
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
...
```
- Push 서비스에서는 제조상태 변경 이벤트를 수신하도록 PolicyHandler 구현

```java
@StreamListener(KafkaProcessor.INPUT)
    public void wheneverProductionChanged_Push(@Payload ProductionChanged productionChanged){

        if(!productionChanged.isMe()) return;
        
        System.out.println("##### listener ProductionChanged: " + productionChanged.toJson());
            
        Push push = new Push();
        push.setOrderId(productionChanged.getOrderId());

        switch (productionChanged.getStatus()){
            case "MAKING":
             push.setMsg("주문하신 음료를 만들고 있습니다.");
             break;

            case "COMPLETED":
             push.setMsg("주문하신 음료가 준비되었습니다.");
             break;
            
            default :
        }

        pushRepository.save(push);
    }
 ```


# 운영

## CI/CD 설정

각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 AWS CodeBuild를 사용하였으며, 
pipeline build script 는 각 프로젝트 폴더 이하에 buildspec.yml 에 포함되었다.
- CodeBuild 기반으로 CI/CD 파이프라인 구성
MSA 서비스별 CodeBuild 프로젝트 생성하여  CI/CD 파이프라인 구성

![ECR](https://i.imgur.com/bdTagTQ.png)

![CodeBuild](https://i.imgur.com/NSug7ru.png)


- Git Hook 연결
연결한 Github의 소스 변경 발생 시 자동으로 빌드 및 배포 되도록 Git Hook 연결 설정

![GitHook](https://i.imgur.com/eyTcEiu.png)

![livenessProve](https://i.imgur.com/N8jNWK6.png)

## Self-healing (Liveness Probe)
Pod는 정상적으로 작동하지만 내부의 어플리케이션이 반응이 없다면, 컨테이너는 의미가 없다.
위와 같은 경우는 어플리케이션의 Liveness probe는 Pod의 상태를 체크하다가, Pod의 상태가 비정상인 경우 kubelet을 통해서 재시작한다.

1. buildspec.yml에 livenessProve 정의
```
        cat  <<EOF | kubectl apply -f -
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: $_PROJECT_NAME
          namespace: $_NAMESPACE
          labels:
            app: $_PROJECT_NAME
        spec:
          replicas: 1
          selector:
            matchLabels:
              app: $_PROJECT_NAME
          template:
            metadata:
              labels:
                app: $_PROJECT_NAME
            spec:
              containers:
                - name: $_PROJECT_NAME
                  image: $AWS_ACCOUNT_ID.dkr.ecr.$_AWS_REGION.amazonaws.com/$_ECR_NAME-$_PROJECT_NAME:latest
                  ports:
                    - containerPort: 8080 
                  imagePullPolicy: Always
                  readinessProbe:
                    httpGet:
                      path: '/actuator/health'
                      port: 8080
                    initialDelaySeconds: 30
                    timeoutSeconds: 2
                    periodSeconds: 5
                    failureThreshold: 10
                  livenessProbe:
                    httpGet:
                      path: '/actuator/health'
                      port: 8080
                    initialDelaySeconds: 120
                    timeoutSeconds: 2
                    periodSeconds: 5
                    failureThreshold: 5   
        EOF
```

2. restart 되었는지 확인
배포는 잘 되었지만 pod가 ready상태가 아닌 경우 계속하여 call하게 되어 RESTART가 증가함

![image](https://i.imgur.com/dWGVKwr.png)



## 무정지 재배포

먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler, CB 설정을 안함 (Readiness Probe 미설정 시 무정지 재배포 가능여부 확인을 위해 buildspec.yml의 Readiness Probe 설정을 제거)

1. siege 설치 및 실행 (동접자 2명으로 120초)
```
kubectl create deploy siege --image=ghcr.io/acmexii/siege-nginx:latest
kubectl exec pod/siege-75d5587bf6-xxdzl -it -- /bin/bash

siege -c2 -t120S -v --content-type "application/json" 'http://a45a441e9767e4fb1826162c5a289b9b-310245936.ca-central-1.elb.amazonaws.com:8080/orders'
```

2. 코드 빌드 및 CI/CD 파이프라인을 통해 새버전으로 재배포

![image](https://i.imgur.com/SGgc7XG.png)

3. siege 결과 확인 (Availability = 100.0%)
배포기간 동안 Availability 가 변화없이 무정지 재배포가 성공한 것 확인

![image](https://i.imgur.com/khF40h7.png) 



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


* url에 configMap 적용
```java
# OrderService.java

@FeignClient(name="OrderManage", url="${api.order.url}")
public interface OrderService {

    @RequestMapping(method= RequestMethod.GET, value="/orders/{orderId}", consumes = "application/json")
    public Order getOrderType(@PathVariable("orderId") Long orderId);

}
```


* kubectl describe 명령어로 configMap 적용여부 확인 
```
kubectl describe pod/productionmanage-54dd595865-tvpgf -n skcc-ns
```


![image](https://i.imgur.com/vumexa5.png)



