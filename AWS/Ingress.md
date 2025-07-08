Ingress:


Kubernetes Ingress is an API resource that allows you to manage external or internal HTTP(S) access to Kubernetes services running in a cluster. Amazon Elastic Load Balancing Application Load Balancer (ALB) is a popular AWS service that load balances incoming traffic at the application layer (layer 7) across multiple targets, such as Amazon EC2 instances, in a region. ALB supports multiple features including host or path based routing, TLS (Transport Layer Security) termination, WebSockets, HTTP/2, AWS WAF (Web Application Firewall) integration, integrated access logs, and health checks.

In this lab exercise, we'll expose our sample application using an ALB with the Kubernetes ingress model.


First lets install the AWS Load Balancer controller using helm:

helm repo add eks-charts https://aws.github.io/eks-charts

helm upgrade --install aws-load-balancer-controller eks-charts/aws-load-balancer-controller \
  --version "${LBC_CHART_VERSION}" \
  --namespace "kube-system" \
  --set "clusterName=${EKS_CLUSTER_NAME}" \
  --set "serviceAccount.name=aws-load-balancer-controller-sa" \
  --set "serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"="$LBC_ROLE_ARN" \
  --wait


Currently there are no Ingress resources in our cluster, which you can check with the following command:

kubectl get ingress -n ui

No resources found in ui namespace.


There are also no Service resources of type LoadBalancer, which you can confirm with the following command:

kubectl get svc -n ui

NAME   TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)   AGE
ui     ClusterIP   10.100.221.103   <none>        80/TCP    29m

Creating the Ingress:

Let's create an Ingress resource with the following configuration:

~/environment/eks-workshop/modules/exposing/ingress/creating-ingress/ingress.yaml

apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ui
  namespace: ui
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health/liveness
spec:
  ingressClassName: alb
  rules:
    - http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: ui
                port:
                  number: 80

A. Use an Ingress kind

B. We can use annotations to configure various behavior of the ALB thats created such as the health checks it performs on the target pods

C. The rules section is used to express how the ALB should route traffic. In this example we route all HTTP requests where the path starts with / to the Kubernetes service called ui on port 80


kubectl apply -k ~/environment/eks-workshop/modules/exposing/ingress/creating-ingress


Let's inspect the Ingress object created:


kubectl get ingress ui -n ui
NAME   CLASS   HOSTS   ADDRESS                                                       PORTS   AGE
ui     alb     *       k8s-ui-ui-5ddc3ba496-1818326871.us-west-2.elb.amazonaws.com   80      9s

The ALB will take several minutes to provision and register its targets so take some time to take a closer look at the ALB provisioned for this Ingress to see how its configured:


aws elbv2 describe-load-balancers --query 'LoadBalancers[?contains(LoadBalancerName, `k8s-ui-ui`) == `true`]'
[
    {
        "LoadBalancerArn": "arn:aws:elasticloadbalancing:us-west-2:085127928691:loadbalancer/app/k8s-ui-ui-5ddc3ba496/f924a735915455fa",
        "DNSName": "k8s-ui-ui-5ddc3ba496-1818326871.us-west-2.elb.amazonaws.com",
        "CanonicalHostedZoneId": "Z1H1FL5HABSF5",
        "CreatedTime": "2025-02-18T11:27:15.641000+00:00",
        "LoadBalancerName": "k8s-ui-ui-5ddc3ba496",
        "Scheme": "internet-facing",
        "VpcId": "vpc-090a1bdc1973b2346",
        "State": {
            "Code": "provisioning"
        },
        "Type": "application",
        "AvailabilityZones": [
            {
                "ZoneName": "us-west-2a",
                "SubnetId": "subnet-04cca48477142c169",
                "LoadBalancerAddresses": []
            },
            {
                "ZoneName": "us-west-2c",
                "SubnetId": "subnet-0802b1584908be431",
                "LoadBalancerAddresses": []
            },
            {
                "ZoneName": "us-west-2b",
                "SubnetId": "subnet-08252b7e756d03f3a",
                "LoadBalancerAddresses": []
            }
        ],
        "SecurityGroups": [
            "sg-05668b707d8758a91",
            "sg-08351145595d97113"
        ],
        "IpAddressType": "ipv4",
        "EnablePrefixForIpv6SourceNat": "off"
    }
]


What does this tell us?

- The ALB is accessible over the public internet
- It uses the public subnets in our VPC


Inspect the targets in the target group that was created by the controller:


ALB_ARN=$(aws elbv2 describe-load-balancers --query 'LoadBalancers[?contains(LoadBalancerName, `k8s-ui-ui`) == `true`].LoadBalancerArn' | jq -r '.[0]')
TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups --load-balancer-arn $ALB_ARN | jq -r '.TargetGroups[0].TargetGroupArn')
aws elbv2 describe-target-health --target-group-arn $TARGET_GROUP_ARN
{
    "TargetHealthDescriptions": [
        {
            "Target": {
                "Id": "10.42.123.224",
                "Port": 8080,
                "AvailabilityZone": "us-west-2a"
            },
            "HealthCheckPort": "8080",
            "TargetHealth": {
                "State": "healthy"
            },
            "AdministrativeOverride": {
                "State": "no_override",
                "Reason": "AdministrativeOverride.NoOverride",
                "Description": "No override is currently active on target"
            }
        }
    ]
}

Since we specified using IP mode in our Ingress object, the target is registered using the IP address of the ui pod and the port on which it serves traffic.


Get the URL from the Ingress resource:


kubectl get ingress -n ui ui -o jsonpath="{.status.loadBalancer.ingress[*].hostname}{'\n'}"

To wait until the load balancer has finished provisioning you can run this command:

wait-for-lb $(kubectl get ingress -n ui ui -o jsonpath="{.status.loadBalancer.ingress[*].hostname}{'\n'}")
And access it in your web browser. You will see the UI from the web store displayed and will be able to navigate around the site as a user.




Multiple Ingress pattern:


It's common to leverage multiple Ingress objects in the same EKS cluster, for example to expose multiple different workloads. By default each Ingress will result in the creation of a separate ALB, but we can leverage the IngressGroup feature which enables you to group multiple Ingress resources together. The controller will automatically merge Ingress rules for all Ingresses within IngressGroup and support them with a single ALB. In addition, most annotations defined on an Ingress only apply to the paths defined by that Ingress.

In this example, we'll expose the catalog API out through the same ALB as the ui component, leveraging path-based routing to dispatch requests to the appropriate Kubernetes service. Let's check we can't already access the catalog API:

ADDRESS=$(kubectl get ingress -n ui ui -o jsonpath="{.status.loadBalancer.ingress[*].hostname}{'\n'}")
curl $ADDRESS/catalogue


The first thing we'll do is re-create the Ingress for ui component adding the annotation alb.ingress.kubernetes.io/group.name:

~/environment/eks-workshop/modules/exposing/ingress/multiple-ingress/ingress-ui.yaml

apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ui
  namespace: ui
  labels:
    app.kubernetes.io/created-by: eks-workshop
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health/liveness
    alb.ingress.kubernetes.io/group.name: retail-app-group
spec:
  ingressClassName: alb
  rules:
    - http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: ui
                port:
                  number: 80


Now, let's create a separate Ingress for the catalog component that also leverages the same group.name:


~/environment/eks-workshop/modules/exposing/ingress/multiple-ingress/ingress-catalog.yaml

apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: catalog
  namespace: catalog
  labels:
    app.kubernetes.io/created-by: eks-workshop
  annotations:
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/healthcheck-path: /health
    alb.ingress.kubernetes.io/group.name: retail-app-group
spec:
  ingressClassName: alb
  rules:
    - http:
        paths:
          - path: /catalogue
            pathType: Prefix
            backend:
              service:
                name: catalog
                port:
                  number: 80

This ingress is also configuring rules to route requests prefixed with /catalogue to the catalog component.

Apply these manifests to the cluster:


kubectl apply -k ~/environment/eks-workshop/modules/exposing/ingress/multiple-ingress

We'll now have two separate Ingress objects in our cluster:

kubectl get ingress -l app.kubernetes.io/created-by=eks-workshop -A

NAMESPACE   NAME      CLASS   HOSTS   ADDRESS                                                              PORTS   AGE
catalog     catalog   alb     *       k8s-retailappgroup-2c24c1c4bc-17962260.us-west-2.elb.amazonaws.com   80      2m21s
ui          ui        alb     *       k8s-retailappgroup-2c24c1c4bc-17962260.us-west-2.elb.amazonaws.com   80      2m21s


Notice that the ADDRESS of both are the same URL, which is because both of these Ingress objects are being grouped together behind the same ALB.

We can take a look at the ALB listener to see how this works:

ALB_ARN=$(aws elbv2 describe-load-balancers --query 'LoadBalancers[?contains(LoadBalancerName, `k8s-retailappgroup`) == `true`].LoadBalancerArn' | jq -r '.[0]')
LISTENER_ARN=$(aws elbv2 describe-listeners --load-balancer-arn $ALB_ARN | jq -r '.Listeners[0].ListenerArn')
aws elbv2 describe-rules --listener-arn $LISTENER_ARN
{
    "Rules": [
        {
            "RuleArn": "arn:aws:elasticloadbalancing:us-west-2:085127928691:listener-rule/app/k8s-retailappgroup-085b071a94/4b5e95ee8ca531a1/8445e69c5b4db7cb/c5c481bcce4b4d47",
            "Priority": "1",
            "Conditions": [
                {
                    "Field": "path-pattern",
                    "Values": [
                        "/catalogue",
                        "/catalogue/*"
                    ],
                    "PathPatternConfig": {
                        "Values": [
                            "/catalogue",
                            "/catalogue/*"
                        ]
                    }
                }
            ],
            "Actions": [
                {
                    "Type": "forward",
                    "TargetGroupArn": "arn:aws:elasticloadbalancing:us-west-2:085127928691:targetgroup/k8s-catalog-catalog-c9c0b716dd/f9aeb289ad57849e",
                    "Order": 1,
                    "ForwardConfig": {
                        "TargetGroups": [
                            {
                                "TargetGroupArn": "arn:aws:elasticloadbalancing:us-west-2:085127928691:targetgroup/k8s-catalog-catalog-c9c0b716dd/f9aeb289ad57849e",
                                "Weight": 1
                            }
                        ],
                        "TargetGroupStickinessConfig": {
                            "Enabled": false
                        }
                    }
                }
            ],
            "IsDefault": false
        },
        {
            "RuleArn": "arn:aws:elasticloadbalancing:us-west-2:085127928691:listener-rule/app/k8s-retailappgroup-085b071a94/4b5e95ee8ca531a1/8445e69c5b4db7cb/7d5f54b852b510e7",
            "Priority": "2",
            "Conditions": [
                {
                    "Field": "path-pattern",
                    "Values": [
                        "/*"
                    ],
                    "PathPatternConfig": {
                        "Values": [
                            "/*"
                        ]
                    }
                }
            ],
            "Actions": [
                {
                    "Type": "forward",
                    "TargetGroupArn": "arn:aws:elasticloadbalancing:us-west-2:085127928691:targetgroup/k8s-ui-ui-eaa5e82d98/98021d2bbb11be61",
                    "Order": 1,
                    "ForwardConfig": {
                        "TargetGroups": [
                            {
                                "TargetGroupArn": "arn:aws:elasticloadbalancing:us-west-2:085127928691:targetgroup/k8s-ui-ui-eaa5e82d98/98021d2bbb11be61",
                                "Weight": 1
                            }
                        ],
                        "TargetGroupStickinessConfig": {
                            "Enabled": false
                        }
                    }
                }
            ],
            "IsDefault": false
        },
        {
            "RuleArn": "arn:aws:elasticloadbalancing:us-west-2:085127928691:listener-rule/app/k8s-retailappgroup-085b071a94/4b5e95ee8ca531a1/8445e69c5b4db7cb/a83f18441e7e5624",
            "Priority": "default",
            "Conditions": [],
            "Actions": [
                {
                    "Type": "fixed-response",
                    "Order": 1,
                    "FixedResponseConfig": {
                        "StatusCode": "404",
                        "ContentType": "text/plain"
                    }
                }
            ],
            "IsDefault": true
        }
    ]
}

The output of this command will illustrate that:

Requests with path prefix /catalogue will get sent to a target group for the catalog service
Everything else will get sent to a target group for the ui service
As a default backup there is a 404 for any requests that happen to fall through the cracks


To wait until the load balancer has finished provisioning you can run this command:


wait-for-lb $(kubectl get ingress -n ui ui -o jsonpath="{.status.loadBalancer.ingress[*].hostname}{'\n'}")


Try accessing the new Ingress URL in the browser as before to check the web UI still works:


kubectl get ingress -n ui ui -o jsonpath="{.status.loadBalancer.ingress[*].hostname}{'\n'}"


Now try accessing the specific path we directed to the catalog service:

ADDRESS=$(kubectl get ingress -n ui ui -o jsonpath="{.status.loadBalancer.ingress[*].hostname}{'\n'}")
curl $ADDRESS/catalogue | jq .


 % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  1130  100  1130    0     0   150k      0 --:--:-- --:--:-- --:--:--  157k
[
  {
    "id": "510a0d7e-8e83-4193-b483-e27e09ddc34d",
    "name": "Gentleman",
    "description": "Touch of class for a bargain.",
    "imageUrl": "/assets/gentleman.jpg",
    "price": 795,
    "count": 51,
    "tag": [
      "dress"
    ]
  },
  {
    "id": "6d62d909-f957-430e-8689-b5129c0bb75e",
    "name": "Pocket Watch",
    "description": "Properly dapper.",
    "imageUrl": "/assets/pocket_watch.jpg",
    "price": 385,
    "count": 33,
    "tag": [
      "dress"
    ]
  },
  {
    "id": "808a2de1-1aaa-4c25-a9b9-6612e8f29a38",
    "name": "Chronograf Classic",
    "description": "Spend that IPO money",
    "imageUrl": "/assets/chrono_classic.jpg",
    "price": 5100,
    "count": 9,
    "tag": [
      "dress",
      "luxury"
    ]
  },
  {
    "id": "a0a4f044-b040-410d-8ead-4de0446aec7e",
    "name": "Wood Watch",
    "description": "Looks like a tree",
    "imageUrl": "/assets/wood_watch.jpg",
    "price": 50,
    "count": 115,
    "tag": [
      "casual"
    ]
  },
  {
    "id": "ee3715be-b4ba-11ea-b3de-0242ac130004",
    "name": "Smart 3.0",
    "description": "Can tell you what you want for breakfast",
    "imageUrl": "/assets/smart_1.jpg",
    "price": 650,
    "count": 9,
    "tag": [
      "smart",
      "dress"
    ]
  },
  {
    "id": "f4ebd070-b4ba-11ea-b3de-0242ac130004",
    "name": "FitnessX",
    "description": "Touch of class for a bargain.",
    "imageUrl": "/assets/smart_2.jpg",
    "price": 180,
    "count": 76,
    "tag": [
      "smart",
      "dress"
    ]
  }
]

You'll receive back a JSON payload from the catalog service, demonstrating that we've been able to expose multiple Kubernetes services via the same ALB.







