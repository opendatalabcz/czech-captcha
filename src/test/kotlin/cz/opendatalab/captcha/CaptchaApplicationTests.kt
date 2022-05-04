package cz.opendatalab.captcha

import cz.opendatalab.captcha.datamanagement.objectmetadata.LabelGroupRepository
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadataRepository
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectCatalogue
import cz.opendatalab.captcha.siteconfig.SiteConfigRepository
import cz.opendatalab.captcha.user.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
class CaptchaApplicationTests(@Autowired @MockBean val userRepository: UserRepository,
							  @Autowired @MockBean val catalog: ObjectCatalogue,
							  @Autowired @MockBean val objectMetadataRepo: ObjectMetadataRepository,
							  @Autowired @MockBean val siteConfigRepo: SiteConfigRepository,
							  @Autowired @MockBean val labelGroupRepo: LabelGroupRepository
) {

	@Test
	fun contextLoads() {
	}

}
