# Open-source CAPTCHA

The main purpose of this CAPTCHA application is to provide CAPTCHA as a service to secure websites from malicious bots.

One of the problems with CAPTCHAs is that they are annoying their users and take valuable time from them.
The goal was to create CAPTCHA application, which would allow the website providers to set up the CAPTCHA in a way, that it is design specifically for their customers, to make the CAPTCHA as painless as possible.
Another goal of this application was to utilize the valuable effort spent by solving the CAPTCHA challenges and to allow website providers use this human computing power of their customers for creating a value.

This application provides CAPTCHA as service and was implemented in a way that one deployment can serve multiple different website providers. The website providers are able to choose from different types of verification methods, which can be simply added by the CAPTCHA providers, due to devised architecture. Further the CAPTCHA service users are able to configure each verification method to their specific needs. Depending on the given verification method, the CAPTCHA service users can use their own data like images. Further the users are able to utilize the human computing of their users with the help of services, which enable verification methods to leverage this power. These services include labeling mechanism or memory, where each verification method can store its data for every file object.

## Verification methods

All the described system properties are demonstrated on an implementation of two default verification methods.
First method is simple well known **text-based CAPTCHA**, where users have to recognize type randomly generated text that is then distorted and presented as an image.

The second implemented verification method is **image-based selection CAPTCHA**, where users are presented with set of images and need to select those with a property specified in the task description. On the second verification method we demonstrate the application's ability to utilize human computing.

## Example usage
To better understand how this application is used, we will demonstrate its usage on an example:

First there is a **provider**, who deploys this application and creates verification methods.

Then lets have a zoo provider, who wants to secure his online ticket reservation with our CAPTCHA. This zoo provider would also like to annotate his big catalogue of animal images - to know which picture contains which animals.

He adds his images to the CAPTCHA application and sets up the labels he wants to assign to these images.
After that he creates configuration for his website in the CAPTCHA application. In this configuration he first chooses the default image-based CAPTCHA (that has labeling capability) as a verification method. Then he specifies how the verification tasks should be generated - that he wants to use his animal images and his created labels.

Finally, he has to implement the CAPTCHA service in his application.

The benefits of him using the CAPTCHA, are that he is able to security mechanism to his reservation form. He makes the CAPTCHA to be as little annoyance as possible, by choosing specific task for his customers. He is also able to use the CAPTCHA annoyance for a practical purpose -- annotation of his photos.


## Deployment

Currently, it is possible to build and deploy the application only using docker compose -- without installing any other dependencies. Make sure you set up environment variables needed from docker-compose.yml file.

```
# 1. Runs tests
# 2. Builds the jar file from Kotlin source files
# 3. Build the docker image
docker-compose build

# Start the application - Runs and connects the containers
docker-compose -p captcha up -d

# Stops the application
docker compose -p captcha stop
```

To run the application with default configuration, use the default configuration in .env-example file.

```
# Start the application with default configuration
docker-compose --env-file .env-example  -p captcha up -d
```
